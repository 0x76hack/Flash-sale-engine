package com.flashsale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PurchaseRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID productId;

    @NotBlank
    private String idempotencyKey;
}