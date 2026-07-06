package com.ionista.repository;

import com.ionista.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    Optional<ProductImage> findByIdAndProductId(Long id, Long productId);
}
