package com.ionista.entity;

import com.ionista.common.BaseEntity;
import com.ionista.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "coupon_code", length = 40)
    private String couponCode;

    @Column(name = "coupon_discount_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;

    @Column(name = "offer_discount_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal offerDiscountAmount = BigDecimal.ZERO;

    @Column(name = "loyalty_points_redeemed", nullable = false)
    @Builder.Default
    private int loyaltyPointsRedeemed = 0;

    @Column(name = "loyalty_discount_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal loyaltyDiscountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "placed_at", nullable = false)
    private LocalDateTime placedAt;

    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "ship_full_name", nullable = false, length = 100)
    private String shipFullName;

    @Column(name = "ship_phone", nullable = false, length = 20)
    private String shipPhone;

    @Column(name = "ship_line1", nullable = false, length = 200)
    private String shipLine1;

    @Column(name = "ship_line2", length = 200)
    private String shipLine2;

    @Column(name = "ship_city", nullable = false, length = 100)
    private String shipCity;

    @Column(name = "ship_state", nullable = false, length = 100)
    private String shipState;

    @Column(name = "ship_postal_code", nullable = false, length = 20)
    private String shipPostalCode;

    @Column(name = "ship_country", nullable = false, length = 100)
    private String shipCountry;

    @OneToMany(mappedBy = "order", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;
}
