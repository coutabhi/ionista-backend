package com.ionista.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyBalanceResponse {

    private int points;
    private String referralCode;
    private Long referredByUserId;
}
