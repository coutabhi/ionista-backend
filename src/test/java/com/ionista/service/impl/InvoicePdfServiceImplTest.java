package com.ionista.service.impl;

import com.ionista.entity.Order;
import com.ionista.entity.OrderItem;
import com.ionista.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class InvoicePdfServiceImplTest {

    private final InvoicePdfServiceImpl service = new InvoicePdfServiceImpl();

    private User buildUser() {
        User user = User.builder().email("jane@example.com").build();
        user.setId(1L);
        return user;
    }

    private Order.OrderBuilder baseOrder() {
        return Order.builder()
                .user(buildUser())
                .subtotal(BigDecimal.valueOf(1000))
                .couponDiscountAmount(BigDecimal.ZERO)
                .offerDiscountAmount(BigDecimal.ZERO)
                .loyaltyDiscountAmount(BigDecimal.ZERO)
                .loyaltyPointsRedeemed(0)
                .shippingFee(BigDecimal.valueOf(50))
                .totalAmount(BigDecimal.valueOf(1050))
                .placedAt(LocalDateTime.of(2026, 7, 12, 10, 0))
                .shipFullName("Jane Doe")
                .shipPhone("9999999999")
                .shipLine1("123 Street")
                .shipCity("Jaipur")
                .shipState("Rajasthan")
                .shipPostalCode("302001")
                .shipCountry("India");
    }

    private OrderItem buildItem(Order order) {
        OrderItem item = OrderItem.builder()
                .order(order)
                .productNameSnapshot("Hand Block Kurti")
                .skuSnapshot("SKU1")
                .sizeSnapshot("M")
                .colorSnapshot("Indigo")
                .priceAtPurchase(BigDecimal.valueOf(500))
                .quantity(2)
                .build();
        item.setId(1L);
        return item;
    }

    @Test
    void generateInvoice_returnsNonEmptyPdfBytes_forOrderWithItems() {
        Order order = baseOrder().build();
        order.setId(200L);
        order.setOrderItems(List.of(buildItem(order)));

        byte[] pdf = service.generateInvoice(order);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void generateInvoice_handlesOrderWithNoDiscounts() {
        Order order = baseOrder().build();
        order.setId(201L);
        order.setOrderItems(List.of(buildItem(order)));

        assertThatCode(() -> service.generateInvoice(order)).doesNotThrowAnyException();
    }

    @Test
    void generateInvoice_handlesOrderWithAllDiscountTypesApplied() {
        Order order = baseOrder()
                .couponCode("SAVE100")
                .couponDiscountAmount(BigDecimal.valueOf(100))
                .offerDiscountAmount(BigDecimal.valueOf(50))
                .loyaltyPointsRedeemed(20)
                .loyaltyDiscountAmount(BigDecimal.valueOf(20))
                .build();
        order.setId(202L);
        order.setOrderItems(List.of(buildItem(order)));

        assertThatCode(() -> service.generateInvoice(order)).doesNotThrowAnyException();
    }
}
