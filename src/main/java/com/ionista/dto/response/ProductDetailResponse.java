package com.ionista.dto.response;

import com.ionista.enums.Gender;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailResponse {

    private Long id;
    private String name;
    private String description;
    private String brand;
    private Long categoryId;
    private String categoryName;
    private Gender gender;
    private BigDecimal basePrice;
    private BigDecimal discountPrice;
    private BigDecimal effectivePrice;
    private String sku;
    private String slug;
    private boolean active;
    private List<ProductVariantResponse> variants;
    private List<ProductImageResponse> images;
}
