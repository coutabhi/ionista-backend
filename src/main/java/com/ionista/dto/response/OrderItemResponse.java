package com.ionista.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long productId;
    private String productName;
    private String sku;
    private String size;
    private String color;
    private BigDecimal priceAtPurchase;
    private int quantity;
    private BigDecimal lineTotal;
}
