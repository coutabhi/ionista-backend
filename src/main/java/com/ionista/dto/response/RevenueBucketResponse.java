package com.ionista.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueBucketResponse {

    private String period;
    private BigDecimal amount;
}
