package com.ionista.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private Long id;
    private Long variantId;
    private Long productId;
    private String productName;
    private String imageUrl;
    private String size;
    private String color;
    private int stockAvailable;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
}
