package com.ionista.repository;

import com.ionista.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductVariantId(Long cartId, Long variantId);
    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);
    void deleteByCartId(Long cartId);
}
