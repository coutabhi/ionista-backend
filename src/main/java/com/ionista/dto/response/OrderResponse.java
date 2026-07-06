package com.ionista.dto.response;

import com.ionista.enums.OrderStatus;
import com.ionista.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private BigDecimal subtotal;
    private String couponCode;
    private BigDecimal couponDiscountAmount;
    private BigDecimal offerDiscountAmount;
    private int loyaltyPointsRedeemed;
    private BigDecimal loyaltyDiscountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private LocalDateTime placedAt;
    private String shipFullName;
    private String shipPhone;
    private String shipLine1;
    private String shipLine2;
    private String shipCity;
    private String shipState;
    private String shipPostalCode;
    private String shipCountry;
    private List<OrderItemResponse> items;
}
