package com.ionista.repository;

import com.ionista.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Category> findByParentId(Long parentId);
    List<Category> findByParentIsNull();
}
