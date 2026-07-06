package com.ionista.controller;

import com.ionista.dto.request.ApplyCouponRequest;
import com.ionista.dto.request.CouponRequest;
import com.ionista.dto.response.CouponResponse;
import com.ionista.dto.response.CouponValidationResponse;
import com.ionista.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/api/v1/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CouponResponse>> listAll() {
        return ResponseEntity.ok(couponService.listAll());
    }

    @PostMapping("/api/v1/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.create(request));
    }

    @PutMapping("/api/v1/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.update(id, request));
    }

    @DeleteMapping("/api/v1/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/coupons/apply")
    public ResponseEntity<CouponValidationResponse> apply(@Valid @RequestBody ApplyCouponRequest request) {
        return ResponseEntity.ok(couponService.validate(request.getCode()));
    }
}
