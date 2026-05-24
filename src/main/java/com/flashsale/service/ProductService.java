package com.flashsale.service;

import com.flashsale.entity.Product;
import com.flashsale.repository.ProductRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;

    public ProductService(
            ProductRepository productRepository,
            StringRedisTemplate redisTemplate
    ) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
    }

    public Product createProduct(Product product) {

        Product saved = productRepository.save(product);

        redisTemplate.opsForValue().set(
                "inventory:" + saved.getId(),
                String.valueOf(saved.getInventory())
        );

        return saved;
    }

    public List<Product> getProducts() {
        return productRepository.findAll();
    }
}