package com.flashsale.service;

import com.flashsale.dto.PurchaseResponse;
import com.flashsale.entity.Order;
import com.flashsale.entity.Product;
import com.flashsale.exception.DuplicatePurchaseException;
import com.flashsale.exception.OutOfStockException;
import com.flashsale.exception.RateLimitExceededException;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.ProductRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PurchaseService {

    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public PurchaseService(StringRedisTemplate redisTemplate,
                           ProductRepository productRepository,
                           OrderRepository orderRepository) {
        this.redisTemplate = redisTemplate;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public PurchaseResponse purchase(UUID userId, UUID productId, String idempotencyKey) {
        if (userId == null || productId == null || idempotencyKey == null
                || idempotencyKey.isBlank() || idempotencyKey.length() > 200) {
            throw new IllegalArgumentException("User, product, and a valid idempotency key are required");
        }

        // Scope idempotency keys to the authenticated user.
        String idempotencyRedisKey = "idempotency:" + userId + ":" + idempotencyKey;
        String cachedOrderId = redisTemplate.opsForValue().get(idempotencyRedisKey);
        if (cachedOrderId != null) {
            return PurchaseResponse.builder().orderId(UUID.fromString(cachedOrderId)).status("SUCCESS").build();
        }

        String rateLimitKey = "rate_limit:" + userId;
        Long requestCount = redisTemplate.opsForValue().increment(rateLimitKey);
        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(rateLimitKey, Duration.ofMinutes(1));
        }
        if (requestCount != null && requestCount > 5) {
            throw new RateLimitExceededException("Too many purchase attempts");
        }

        String purchaseKey = "purchase:" + userId + ":" + productId;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(purchaseKey, "pending", Duration.ofMinutes(5));
        if (!Boolean.TRUE.equals(acquired)) {
            // A duplicate or concurrent attempt must not reserve stock twice.
            String replayId = redisTemplate.opsForValue().get(idempotencyRedisKey);
            if (replayId != null) {
                return PurchaseResponse.builder().orderId(UUID.fromString(replayId)).status("SUCCESS").build();
            }
            throw new DuplicatePurchaseException("User already has a purchase in progress for this product");
        }

        try {
            // Atomic DB-side reservation prevents overselling even if Redis inventory is stale.
            if (productRepository.reserveOneUnit(productId) != 1) {
                throw new OutOfStockException("Product is out of stock or does not exist");
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new OutOfStockException("Product does not exist"));
            Order order = Order.builder()
                    .userId(userId)
                    .productId(productId)
                    .amount(product.getPrice())
                    .createdAt(LocalDateTime.now())
                    .build();
            Order savedOrder = orderRepository.save(order);

            redisTemplate.opsForValue().set(idempotencyRedisKey, savedOrder.getId().toString(),
                    Duration.ofHours(1));
            redisTemplate.opsForValue().set(purchaseKey, savedOrder.getId().toString(),
                    Duration.ofDays(365));
            return PurchaseResponse.builder().orderId(savedOrder.getId()).status("SUCCESS").build();
        } catch (RuntimeException ex) {
            // DB transaction rolls back the inventory update. Remove the temporary guard.
            redisTemplate.delete(purchaseKey);
            throw ex;
        }
    }
}
