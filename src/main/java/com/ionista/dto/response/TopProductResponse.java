package com.ionista.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopProductResponse {

    private Long productId;
    private String name;
    private long unitsSold;
    private BigDecimal revenue;
}
