package com.ionista.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long id;
    private Long userId;
    private String userName;
    private int rating;
    private String comment;
    private boolean verifiedPurchase;
    private LocalDateTime createdAt;
}
