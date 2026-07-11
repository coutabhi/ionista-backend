package com.ionista.service;

import com.ionista.dto.request.EmailCampaignRequest;
import com.ionista.dto.response.EmailCampaignResponse;

import java.util.List;

public interface EmailCampaignService {

    List<EmailCampaignResponse> listAll();

    EmailCampaignResponse createAndSend(EmailCampaignRequest request);
}
