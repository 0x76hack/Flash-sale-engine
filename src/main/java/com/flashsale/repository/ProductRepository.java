package com.flashsale.repository;

import com.flashsale.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * The database is the inventory source of truth. The conditional update
     * serializes competing buyers and can never decrement below zero.
     */
    @Modifying
    @Query("update Product p set p.inventory = p.inventory - 1 where p.id = :id and p.inventory > 0")
    int reserveOneUnit(@Param("id") UUID productId);
}
