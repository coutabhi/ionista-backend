package com.ionista.entity;

import com.ionista.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 20)
    private String size;

    @Column(nullable = false, length = 40)
    private String color;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "price_override", precision = 10, scale = 2)
    private BigDecimal priceOverride;

    @Column(nullable = false, unique = true, length = 70)
    private String sku;

    @Version
    private Long version;
}
