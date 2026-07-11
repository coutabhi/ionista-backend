package com.ionista.controller;

import com.ionista.service.SiteContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SiteContentController {

    private final SiteContentService siteContentService;

    @GetMapping("/api/v1/content")
    public ResponseEntity<Map<String, String>> getAll() {
        return ResponseEntity.ok(siteContentService.getAll());
    }

    @PutMapping("/api/v1/admin/content")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> update(@RequestBody Map<String, String> updates) {
        return ResponseEntity.ok(siteContentService.update(updates));
    }
}
