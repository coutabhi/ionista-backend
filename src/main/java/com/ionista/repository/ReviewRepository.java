package com.ionista.repository;

import com.ionista.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"user"})
    Page<Review> findByProductId(Long productId, Pageable pageable);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
    Optional<Review> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.product.id = :productId")
    double averageRatingByProductId(@Param("productId") Long productId);

    long countByProductId(Long productId);
}
