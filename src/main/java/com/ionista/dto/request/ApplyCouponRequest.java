package com.ionista.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyCouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;
}
