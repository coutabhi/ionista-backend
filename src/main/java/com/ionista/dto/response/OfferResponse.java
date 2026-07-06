package com.ionista.dto.response;

import com.ionista.enums.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferResponse {

    private Long id;
    private String name;
    private DiscountType discountType;
    private BigDecimal value;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private boolean active;
    private List<Long> productIds;
    private List<Long> categoryIds;
}
