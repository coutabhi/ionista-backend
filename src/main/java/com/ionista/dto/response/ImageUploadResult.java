package com.ionista.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadResult {

    private String url;
    private String publicId;
}
