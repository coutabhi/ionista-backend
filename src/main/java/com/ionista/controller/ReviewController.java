package com.ionista.controller;

import com.ionista.dto.request.ReviewRequest;
import com.ionista.dto.response.PageResponse;
import com.ionista.dto.response.ProductRatingSummaryResponse;
import com.ionista.dto.response.ReviewResponse;
import com.ionista.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<PageResponse<ReviewResponse>> listByProduct(@PathVariable Long productId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.listByProduct(productId, pageable));
    }

    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ProductRatingSummaryResponse> ratingSummary(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.ratingSummary(productId));
    }

    @PostMapping("/product/{productId}")
    public ResponseEntity<ReviewResponse> create(@PathVariable Long productId, @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(productId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> update(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
