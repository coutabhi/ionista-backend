package com.ionista.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageResponse {

    private Long id;
    private String url;
    private boolean isPrimary;
    private int sortOrder;
}
