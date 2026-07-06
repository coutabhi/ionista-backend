package com.ionista.dto.response;

import com.ionista.enums.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {

    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal value;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer perUserLimit;
    private int usedCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;
}
