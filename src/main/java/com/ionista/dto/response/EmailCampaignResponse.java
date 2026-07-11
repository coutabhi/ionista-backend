package com.ionista.dto.response;

import com.ionista.enums.EmailCampaignStatus;
import com.ionista.enums.EmailTargetType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailCampaignResponse {

    private Long id;
    private String subject;
    private EmailTargetType targetType;
    private EmailCampaignStatus status;
    private int recipientCount;
    private int failedCount;
    private LocalDateTime sentAt;
}
