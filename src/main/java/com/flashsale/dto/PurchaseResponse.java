package com.flashsale.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PurchaseResponse {

    private UUID orderId;

    private String status;
}