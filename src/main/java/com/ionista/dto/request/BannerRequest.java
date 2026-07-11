package com.ionista.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerRequest {

    @NotBlank(message = "Banner title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    private String subtitle;

    @Size(max = 60, message = "CTA text cannot exceed 60 characters")
    private String ctaText;

    @Size(max = 300, message = "CTA link cannot exceed 300 characters")
    private String ctaLink;

    private Integer sortOrder;

    private Boolean active;
}
