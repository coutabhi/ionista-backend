package com.ionista.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutResponse {

    private Long orderId;
    private String razorpayOrderId;
    private String razorpayKeyId;
    private BigDecimal amount;
    private String currency;
}
