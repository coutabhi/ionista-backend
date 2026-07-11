package com.ionista.controller;

import com.ionista.dto.request.BannerRequest;
import com.ionista.dto.response.BannerResponse;
import com.ionista.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/api/v1/banners/active")
    public ResponseEntity<List<BannerResponse>> listActive() {
        return ResponseEntity.ok(bannerService.listActive());
    }

    @GetMapping("/api/v1/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BannerResponse>> listAll() {
        return ResponseEntity.ok(bannerService.listAll());
    }

    @PostMapping("/api/v1/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerResponse> create(@Valid @RequestBody BannerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bannerService.create(request));
    }

    @PutMapping("/api/v1/admin/banners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerResponse> update(@PathVariable Long id, @Valid @RequestBody BannerRequest request) {
        return ResponseEntity.ok(bannerService.update(id, request));
    }

    @DeleteMapping("/api/v1/admin/banners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/api/v1/admin/banners/{id}/image", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerResponse> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(bannerService.uploadImage(id, file));
    }
}
