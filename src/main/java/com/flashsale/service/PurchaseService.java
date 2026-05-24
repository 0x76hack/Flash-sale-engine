package com.flashsale.service;

import com.flashsale.dto.PurchaseResponse;
import com.flashsale.entity.Order;
import com.flashsale.entity.Product;
import com.flashsale.exception.OutOfStockException;
import com.flashsale.exception.RateLimitExceededException;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.ProductRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.flashsale.exception.DuplicatePurchaseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PurchaseService {

    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public PurchaseService(
            StringRedisTemplate redisTemplate,
            ProductRepository productRepository,
            OrderRepository orderRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public PurchaseResponse purchase(
            UUID userId,
            UUID productId,
            String idempotencyKey
    ) {
        String idempotencyRedisKey =
                "idempotency:" + idempotencyKey;

        String cachedOrderId = redisTemplate.opsForValue()
                .get(idempotencyRedisKey);

        if (cachedOrderId != null) {

            return PurchaseResponse.builder()
                    .orderId(UUID.fromString(cachedOrderId))
                    .status("SUCCESS")
                    .build();
        }

        String rateLimitKey = "rate_limit:" + userId;

        Long requestCount = redisTemplate.opsForValue()
                .increment(rateLimitKey);

        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(
                    rateLimitKey,
                    Duration.ofMinutes(1)
            );
        }

        if (requestCount != null && requestCount > 5) {
            throw new RateLimitExceededException(
                    "Too many purchase attempts"
            );
        }

        String purchaseKey =
                "purchase:" + userId + ":" + productId;

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(
                        purchaseKey,
                        "true",
                        Duration.ofMinutes(5)
                );

        if (Boolean.FALSE.equals(success)) {
            throw new DuplicatePurchaseException(
                    "User already purchased this product"
            );
        }

        String inventoryKey = "inventory:" + productId;

        Long remainingInventory =
                redisTemplate.opsForValue()
                        .decrement(inventoryKey);

        if (remainingInventory == null || remainingInventory < 0) {

            redisTemplate.opsForValue()
                    .increment(inventoryKey);

            throw new OutOfStockException(
                    "Product is out of stock"
            );
        }

        try {

            Product product = productRepository
                    .findById(productId)
                    .orElseThrow();

            Order order = Order.builder()
                    .userId(userId)
                    .productId(productId)
                    .amount(product.getPrice())
                    .createdAt(LocalDateTime.now())
                    .build();

            Order savedOrder = orderRepository.save(order);

            redisTemplate.opsForValue().set(
                    idempotencyRedisKey,
                    savedOrder.getId().toString(),
                    Duration.ofHours(1)
            );

            return PurchaseResponse.builder()
                    .orderId(savedOrder.getId())
                    .status("SUCCESS")
                    .build();

        }   catch (Exception ex) {
            redisTemplate.opsForValue()
                .increment(inventoryKey);

            redisTemplate.delete(purchaseKey);

            throw ex;
        }
    }
}