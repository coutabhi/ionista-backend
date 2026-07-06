package com.ionista.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponApplicationResult {

    private Long couponId;
    private String code;
    private BigDecimal discountAmount;
}
