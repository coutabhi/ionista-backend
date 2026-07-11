package com.ionista.service.impl;

import com.ionista.dto.request.EmailCampaignRequest;
import com.ionista.dto.response.EmailCampaignResponse;
import com.ionista.dto.response.EmailSendResult;
import com.ionista.entity.EmailCampaign;
import com.ionista.entity.User;
import com.ionista.enums.EmailCampaignStatus;
import com.ionista.enums.EmailTargetType;
import com.ionista.exception.BadRequestException;
import com.ionista.repository.EmailCampaignRepository;
import com.ionista.repository.UserRepository;
import com.ionista.service.EmailCampaignService;
import com.ionista.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailCampaignServiceImpl implements EmailCampaignService {

    private final EmailCampaignRepository emailCampaignRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public List<EmailCampaignResponse> listAll() {
        return emailCampaignRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public EmailCampaignResponse createAndSend(EmailCampaignRequest request) {
        List<String> recipients = resolveRecipients(request);
        if (recipients.isEmpty()) {
            throw new BadRequestException("No recipients matched this campaign's target selection");
        }

        EmailSendResult result = emailService.sendBulk(recipients, request.getSubject(), request.getBodyHtml());

        EmailCampaign campaign = EmailCampaign.builder()
                .subject(request.getSubject())
                .bodyHtml(request.getBodyHtml())
                .targetType(request.getTargetType())
                .status(result.getSuccessCount() > 0 ? EmailCampaignStatus.SENT : EmailCampaignStatus.FAILED)
                .recipientCount(result.getSuccessCount())
                .failedCount(result.getFailureCount())
                .sentAt(LocalDateTime.now())
                .build();

        return toResponse(emailCampaignRepository.save(campaign));
    }

    private List<String> resolveRecipients(EmailCampaignRequest request) {
        if (request.getTargetType() == EmailTargetType.ALL) {
            return userRepository.findAllByIsActiveTrue().stream().map(User::getEmail).toList();
        }

        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new BadRequestException("Select at least one customer to send this campaign to");
        }
        return userRepository.findAllById(request.getUserIds()).stream().map(User::getEmail).toList();
    }

    private EmailCampaignResponse toResponse(EmailCampaign campaign) {
        return EmailCampaignResponse.builder()
                .id(campaign.getId())
                .subject(campaign.getSubject())
                .targetType(campaign.getTargetType())
                .status(campaign.getStatus())
                .recipientCount(campaign.getRecipientCount())
                .failedCount(campaign.getFailedCount())
                .sentAt(campaign.getSentAt())
                .build();
    }
}
