package com.ionista.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {

    @NotNull(message = "Address id is required")
    private Long addressId;

    private String couponCode;

    private Integer loyaltyPointsToRedeem;
}
