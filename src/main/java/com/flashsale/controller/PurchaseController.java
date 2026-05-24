package com.flashsale.controller;

import com.flashsale.dto.PurchaseRequest;
import com.flashsale.dto.PurchaseResponse;
import com.flashsale.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchase")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(
            PurchaseService purchaseService
    ) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public PurchaseResponse purchase(
            @Valid @RequestBody PurchaseRequest request
    ) {

        return purchaseService.purchase(
                request.getUserId(),
                request.getProductId(),
                request.getIdempotencyKey()
        );
    }
}