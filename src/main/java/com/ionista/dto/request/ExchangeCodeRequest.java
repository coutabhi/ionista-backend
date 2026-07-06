package com.ionista.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeCodeRequest {

    @NotBlank(message = "Code is required")
    private String code;
}
