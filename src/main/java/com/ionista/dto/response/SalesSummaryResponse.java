package com.ionista.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesSummaryResponse {

    private BigDecimal totalSales;
    private long orderCount;
    private LocalDate from;
    private LocalDate to;
}
