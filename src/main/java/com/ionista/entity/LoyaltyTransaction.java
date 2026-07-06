package com.ionista.entity;

import com.ionista.common.BaseEntity;
import com.ionista.enums.LoyaltyTransactionType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "loyalty_transactions")
public class LoyaltyTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int points;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoyaltyTransactionType type;

    @Column(name = "related_order_id")
    private Long relatedOrderId;

    @Column(length = 255)
    private String description;
}
