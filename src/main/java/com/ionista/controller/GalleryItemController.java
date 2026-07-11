package com.ionista.controller;

import com.ionista.dto.request.GalleryItemRequest;
import com.ionista.dto.response.GalleryItemResponse;
import com.ionista.service.GalleryItemService;
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
public class GalleryItemController {

    private final GalleryItemService galleryItemService;

    @GetMapping("/api/v1/gallery/active")
    public ResponseEntity<List<GalleryItemResponse>> listActive() {
        return ResponseEntity.ok(galleryItemService.listActive());
    }

    @GetMapping("/api/v1/admin/gallery")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GalleryItemResponse>> listAll() {
        return ResponseEntity.ok(galleryItemService.listAll());
    }

    @PostMapping("/api/v1/admin/gallery")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GalleryItemResponse> create(@Valid @RequestBody GalleryItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(galleryItemService.create(request));
    }

    @PutMapping("/api/v1/admin/gallery/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GalleryItemResponse> update(@PathVariable Long id, @Valid @RequestBody GalleryItemRequest request) {
        return ResponseEntity.ok(galleryItemService.update(id, request));
    }

    @DeleteMapping("/api/v1/admin/gallery/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        galleryItemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/api/v1/admin/gallery/{id}/image", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GalleryItemResponse> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(galleryItemService.uploadImage(id, file));
    }
}
