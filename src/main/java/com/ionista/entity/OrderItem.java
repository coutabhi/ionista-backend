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
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant productVariant;

    @Column(name = "product_name_snapshot", nullable = false, length = 150)
    private String productNameSnapshot;

    @Column(name = "sku_snapshot", length = 70)
    private String skuSnapshot;

    @Column(name = "size_snapshot", length = 20)
    private String sizeSnapshot;

    @Column(name = "color_snapshot", length = 40)
    private String colorSnapshot;

    @Column(name = "price_at_purchase", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    @Column(nullable = false)
    private int quantity;
}
