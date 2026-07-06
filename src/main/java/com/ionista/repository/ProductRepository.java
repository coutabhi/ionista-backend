package com.ionista.repository;

import com.ionista.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"category", "images"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "variants", "images"})
    Optional<Product> findBySlug(String slug);

    @EntityGraph(attributePaths = {"category", "variants", "images"})
    Optional<Product> findById(Long id);

    boolean existsBySku(String sku);
    boolean existsBySlug(String slug);
}
