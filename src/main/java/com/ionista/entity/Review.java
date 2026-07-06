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
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int rating;

    @Lob
    @Column
    private String comment;

    @Column(name = "verified_purchase", nullable = false)
    @Builder.Default
    private boolean verifiedPurchase = false;
}
