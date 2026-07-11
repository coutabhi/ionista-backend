package com.ionista.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerResponse {

    private Long id;
    private String title;
    private String subtitle;
    private String ctaText;
    private String ctaLink;
    private String imageUrl;
    private int sortOrder;
    private boolean active;
}
