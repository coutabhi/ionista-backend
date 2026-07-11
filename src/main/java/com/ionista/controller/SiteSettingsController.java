package com.ionista.controller;

import com.ionista.dto.request.SiteSettingsRequest;
import com.ionista.dto.response.SiteSettingsResponse;
import com.ionista.service.SiteSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class SiteSettingsController {

    private final SiteSettingsService siteSettingsService;

    @GetMapping("/api/v1/settings")
    public ResponseEntity<SiteSettingsResponse> get() {
        return ResponseEntity.ok(siteSettingsService.get());
    }

    @PutMapping("/api/v1/admin/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteSettingsResponse> update(@Valid @RequestBody SiteSettingsRequest request) {
        return ResponseEntity.ok(siteSettingsService.update(request));
    }

    @PostMapping(value = "/api/v1/admin/settings/logo", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteSettingsResponse> uploadLogo(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(siteSettingsService.uploadLogo(file));
    }

    @PostMapping(value = "/api/v1/admin/settings/favicon", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteSettingsResponse> uploadFavicon(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(siteSettingsService.uploadFavicon(file));
    }
}
