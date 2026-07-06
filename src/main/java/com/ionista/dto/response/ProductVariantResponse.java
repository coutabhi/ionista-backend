package com.ionista.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponse {

    private Long id;
    private String size;
    private String color;
    private int stockQuantity;
    private BigDecimal priceOverride;
    private String sku;
}
