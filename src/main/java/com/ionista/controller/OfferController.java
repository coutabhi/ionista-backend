package com.ionista.controller;

import com.ionista.dto.request.OfferRequest;
import com.ionista.dto.response.OfferResponse;
import com.ionista.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @GetMapping("/api/v1/offers/active")
    public ResponseEntity<List<OfferResponse>> listActive() {
        return ResponseEntity.ok(offerService.listActive());
    }

    @GetMapping("/api/v1/admin/offers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OfferResponse>> listAll() {
        return ResponseEntity.ok(offerService.listAll());
    }

    @PostMapping("/api/v1/admin/offers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OfferResponse> create(@Valid @RequestBody OfferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(offerService.create(request));
    }

    @PutMapping("/api/v1/admin/offers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OfferResponse> update(@PathVariable Long id, @Valid @RequestBody OfferRequest request) {
        return ResponseEntity.ok(offerService.update(id, request));
    }

    @DeleteMapping("/api/v1/admin/offers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        offerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
