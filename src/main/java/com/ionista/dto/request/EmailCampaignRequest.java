package com.ionista.dto.request;

import com.ionista.enums.EmailTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailCampaignRequest {

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject cannot exceed 200 characters")
    private String subject;

    @NotBlank(message = "Email body is required")
    private String bodyHtml;

    @NotNull(message = "Target type is required")
    private EmailTargetType targetType;

    private List<Long> userIds;
}
