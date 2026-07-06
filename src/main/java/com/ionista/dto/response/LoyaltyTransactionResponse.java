package com.ionista.dto.response;

import com.ionista.enums.LoyaltyTransactionType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTransactionResponse {

    private Long id;
    private int points;
    private LoyaltyTransactionType type;
    private Long relatedOrderId;
    private String description;
    private LocalDateTime createdAt;
}
