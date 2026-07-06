package com.ionista.dto.response;

import com.ionista.enums.Gender;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSummaryResponse {

    private Long id;
    private String name;
    private String slug;
    private String brand;
    private Gender gender;
    private BigDecimal basePrice;
    private BigDecimal discountPrice;
    private BigDecimal effectivePrice;
    private String primaryImageUrl;
    private String categoryName;
    private boolean active;
}
