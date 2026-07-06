package com.ionista.entity;

import com.ionista.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "product_images")
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "public_id", nullable = false, length = 200)
    private String publicId;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean isPrimary = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
