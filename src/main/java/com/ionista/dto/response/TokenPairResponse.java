package com.ionista.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenPairResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
}
