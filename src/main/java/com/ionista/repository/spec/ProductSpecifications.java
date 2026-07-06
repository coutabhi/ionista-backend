package com.ionista.repository.spec;

import com.ionista.entity.Product;
import com.ionista.entity.ProductVariant;
import com.ionista.enums.Gender;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> hasGender(Gender gender) {
        if (gender == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("gender"), gender);
    }

    public static Specification<Product> hasBrand(String brand) {
        if (brand == null || brand.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("brand")), brand.toLowerCase());
    }

    public static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return null;
        }
        return (root, query, cb) -> {
            Expression<BigDecimal> effectivePrice = cb.coalesce(root.get("discountPrice"), root.get("basePrice"));
            if (min != null && max != null) {
                return cb.between(effectivePrice, min, max);
            } else if (min != null) {
                return cb.greaterThanOrEqualTo(effectivePrice, min);
            } else {
                return cb.lessThanOrEqualTo(effectivePrice, max);
            }
        };
    }

    public static Specification<Product> nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String likePattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), likePattern),
                cb.like(cb.lower(root.get("description")), likePattern),
                cb.like(cb.lower(root.get("brand")), likePattern)
        );
    }

    public static Specification<Product> hasSize(String size) {
        if (size == null || size.isBlank()) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Product, ProductVariant> variants = root.join("variants");
            return cb.equal(cb.lower(variants.get("size")), size.toLowerCase());
        };
    }

    public static Specification<Product> hasColor(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Product, ProductVariant> variants = root.join("variants");
            return cb.equal(cb.lower(variants.get("color")), color.toLowerCase());
        };
    }
}
