package com.flashsale.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_orders_idempotency_key", columnNames = "idempotency_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId;

    private UUID productId;

    private BigDecimal amount;

    private LocalDateTime createdAt;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;
}
