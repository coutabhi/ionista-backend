package com.ionista.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItemResponse {

    private Long productId;
    private String name;
    private String slug;
    private String imageUrl;
    private BigDecimal effectivePrice;
    private boolean active;
    private LocalDateTime addedAt;
}
