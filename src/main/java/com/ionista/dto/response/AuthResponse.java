package com.ionista.dto.response;

import com.ionista.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private Long userId;
    private String email;
    private Role role;
    private String token;
    private String refreshToken;
    private String message;
}