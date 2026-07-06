package com.ionista.mapper;

import com.ionista.dto.response.OrderItemResponse;
import com.ionista.dto.response.OrderResponse;
import com.ionista.entity.Order;
import com.ionista.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus())
                .paymentStatus(order.getPayment() != null ? order.getPayment().getStatus() : null)
                .subtotal(order.getSubtotal())
                .couponCode(order.getCouponCode())
                .couponDiscountAmount(order.getCouponDiscountAmount())
                .offerDiscountAmount(order.getOfferDiscountAmount())
                .loyaltyPointsRedeemed(order.getLoyaltyPointsRedeemed())
                .loyaltyDiscountAmount(order.getLoyaltyDiscountAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .placedAt(order.getPlacedAt())
                .shipFullName(order.getShipFullName())
                .shipPhone(order.getShipPhone())
                .shipLine1(order.getShipLine1())
                .shipLine2(order.getShipLine2())
                .shipCity(order.getShipCity())
                .shipState(order.getShipState())
                .shipPostalCode(order.getShipPostalCode())
                .shipCountry(order.getShipCountry())
                .items(order.getOrderItems().stream().map(this::toItemResponse).toList())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductNameSnapshot())
                .sku(item.getSkuSnapshot())
                .size(item.getSizeSnapshot())
                .color(item.getColorSnapshot())
                .priceAtPurchase(item.getPriceAtPurchase())
                .quantity(item.getQuantity())
                .lineTotal(item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }
}
