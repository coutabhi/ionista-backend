package com.ionista.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponValidationResponse {

    private boolean valid;
    private BigDecimal discountAmount;
    private String message;
}
