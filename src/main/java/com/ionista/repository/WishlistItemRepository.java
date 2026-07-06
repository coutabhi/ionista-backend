package com.ionista.repository;

import com.ionista.entity.WishlistItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    @EntityGraph(attributePaths = {"product", "product.images"})
    List<WishlistItem> findByUserId(Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
    void deleteByUserIdAndProductId(Long userId, Long productId);
}
