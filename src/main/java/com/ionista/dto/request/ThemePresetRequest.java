package com.ionista.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThemePresetRequest {

    @NotBlank(message = "Theme name is required")
    @Size(max = 80, message = "Theme name cannot exceed 80 characters")
    private String name;

    @NotBlank(message = "Primary color is required")
    private String primaryColor;

    @NotBlank(message = "Secondary color is required")
    private String secondaryColor;

    @NotBlank(message = "Accent color is required")
    private String accentColor;

    @NotBlank(message = "Background color is required")
    private String backgroundColor;

    @NotBlank(message = "Text color is required")
    private String textColor;
}
