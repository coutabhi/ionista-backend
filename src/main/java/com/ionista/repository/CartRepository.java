package com.ionista.repository;

import com.ionista.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"items", "items.productVariant", "items.productVariant.product", "items.productVariant.product.images"})
    Optional<Cart> findByUserId(Long userId);
}
