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
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public PurchaseResponse purchase(UUID userId, UUID productId, String idempotencyKey) {
        if (userId == null || productId == null) {
            throw new IllegalArgumentException("userId and productId are required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("A non-empty idempotency key of at most 128 characters is required");
        }

        // Idempotency is durable in the same database transaction as inventory and order creation.
        Order previousOrder = orderRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (previousOrder != null) {
            if (!userId.equals(previousOrder.getUserId()) || !productId.equals(previousOrder.getProductId())) {
                throw new IllegalArgumentException("Idempotency key was already used for a different purchase");
            }
            return response(previousOrder);
        }

        String rateLimitKey = "rate_limit:" + userId;
        Long requestCount = redisTemplate.opsForValue().increment(rateLimitKey);
        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(rateLimitKey, Duration.ofMinutes(1));
        }
        if (requestCount != null && requestCount > 5) {
            throw new RateLimitExceededException("Too many purchase attempts");
        }

        // PostgreSQL is authoritative for inventory. Redis inventory counters can drift
        // when one store commits and the other fails, so no stock is decremented in Redis.
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (product.getInventory() == null || product.getInventory() <= 0) {
            throw new OutOfStockException("Product is out of stock");
        }

        LocalDateTime now = LocalDateTime.now();
        if (product.getSaleStart() != null && now.isBefore(product.getSaleStart())) {
            throw new IllegalArgumentException("Sale has not started");
        }
        if (product.getSaleEnd() != null && now.isAfter(product.getSaleEnd())) {
            throw new IllegalArgumentException("Sale has ended");
        }

        boolean alreadyPurchased = orderRepository.findAll().stream()
                .anyMatch(order -> userId.equals(order.getUserId()) && productId.equals(order.getProductId()));
        if (alreadyPurchased) {
            throw new DuplicatePurchaseException("User already purchased this product");
        }

        product.setInventory(product.getInventory() - 1);
        productRepository.save(product);

        Order order = Order.builder()
                .userId(userId)
                .productId(productId)
                .amount(product.getPrice())
                .createdAt(now)
                .idempotencyKey(idempotencyKey)
                .build();

        // Inventory and order commit atomically in PostgreSQL; failures roll both back.
        return response(orderRepository.save(order));
    }

    private PurchaseResponse response(Order order) {
        return PurchaseResponse.builder()
                .orderId(order.getId())
                .status("SUCCESS")
                .build();
    }
}
