package com.ionista.repository;

import com.ionista.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);
    Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);
    boolean existsBySku(String sku);
}
