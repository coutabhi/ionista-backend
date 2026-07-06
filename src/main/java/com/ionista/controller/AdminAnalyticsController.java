package com.ionista.controller;

import com.ionista.dto.response.*;
import com.ionista.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/sales-summary")
    public ResponseEntity<SalesSummaryResponse> salesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.salesSummary(from, to));
    }

    @GetMapping("/revenue")
    public ResponseEntity<List<RevenueBucketResponse>> revenue(
            @RequestParam(required = false, defaultValue = "day") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.revenue(groupBy, from, to));
    }

    @GetMapping("/orders/status-counts")
    public ResponseEntity<List<OrderStatusCountResponse>> orderStatusCounts() {
        return ResponseEntity.ok(analyticsService.orderStatusCounts());
    }

    @GetMapping("/products/top-selling")
    public ResponseEntity<List<TopProductResponse>> topSellingProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.topSellingProducts(from, to, limit));
    }

    @GetMapping("/users/stats")
    public ResponseEntity<UserStatsResponse> userStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.userStats(from, to));
    }
}
