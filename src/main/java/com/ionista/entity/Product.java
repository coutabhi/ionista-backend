package com.ionista.entity;

import com.ionista.common.BaseEntity;
import com.ionista.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(length = 100)
    private String brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @Column(nullable = false, unique = true, length = 60)
    private String sku;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();
}
