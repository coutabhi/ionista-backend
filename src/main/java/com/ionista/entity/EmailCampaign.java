package com.ionista.entity;

import com.ionista.common.BaseEntity;
import com.ionista.enums.EmailCampaignStatus;
import com.ionista.enums.EmailTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "email_campaigns")
public class EmailCampaign extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String subject;

    @Lob
    @Column(name = "body_html", nullable = false)
    private String bodyHtml;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private EmailTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailCampaignStatus status;

    @Column(name = "recipient_count", nullable = false)
    @Builder.Default
    private int recipientCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    private int failedCount = 0;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
