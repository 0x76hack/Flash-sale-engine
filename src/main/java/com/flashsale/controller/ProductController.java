package com.flashsale.controller;

import com.flashsale.dto.ProductRequest;
import com.flashsale.dto.ProductResponse;
import com.flashsale.entity.Product;
import com.flashsale.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(
            ProductService productService
    ) {
        this.productService = productService;
    }

    @PostMapping
    public ProductResponse createProduct(
            @Valid @RequestBody ProductRequest request
    ) {

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .inventory(request.getInventory())
                .build();

        Product saved = productService.createProduct(product);

        return ProductResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .price(saved.getPrice())
                .inventory(saved.getInventory())
                .build();
    }

    @GetMapping
    public List<Product> getProducts() {
        return productService.getProducts();
    }
}