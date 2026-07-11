package com.ionista.controller;

import com.ionista.dto.request.EmailCampaignRequest;
import com.ionista.dto.response.EmailCampaignResponse;
import com.ionista.service.EmailCampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/campaigns")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class EmailCampaignController {

    private final EmailCampaignService emailCampaignService;

    @GetMapping
    public ResponseEntity<List<EmailCampaignResponse>> listAll() {
        return ResponseEntity.ok(emailCampaignService.listAll());
    }

    @PostMapping
    public ResponseEntity<EmailCampaignResponse> createAndSend(@Valid @RequestBody EmailCampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(emailCampaignService.createAndSend(request));
    }
}
