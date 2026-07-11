package com.ionista.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GalleryItemResponse {

    private Long id;
    private String title;
    private String caption;
    private String imageUrl;
    private int sortOrder;
    private boolean active;
}
