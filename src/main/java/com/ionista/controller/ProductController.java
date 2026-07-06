package com.ionista.controller;

import com.ionista.dto.request.ProductRequest;
import com.ionista.dto.request.ProductVariantRequest;
import com.ionista.dto.response.PageResponse;
import com.ionista.dto.response.ProductDetailResponse;
import com.ionista.dto.response.ProductSummaryResponse;
import com.ionista.enums.Gender;
import com.ionista.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/api/v1/products")
    public ResponseEntity<PageResponse<ProductSummaryResponse>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(productService.list(categoryId, gender, brand, minPrice, maxPrice, size, color, keyword, pageable));
    }

    @GetMapping("/api/v1/products/{id}")
    public ResponseEntity<ProductDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/api/v1/products/slug/{slug}")
    public ResponseEntity<ProductDetailResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getBySlug(slug));
    }

    @GetMapping("/api/v1/products/category/{categoryId}")
    public ResponseEntity<PageResponse<ProductSummaryResponse>> listByCategory(
            @PathVariable Long categoryId, Pageable pageable) {
        return ResponseEntity.ok(productService.listByCategory(categoryId, pageable));
    }

    @PostMapping("/api/v1/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDetailResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/api/v1/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDetailResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/api/v1/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/admin/products/{id}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDetailResponse> addVariant(@PathVariable Long id, @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addVariant(id, request));
    }

    @PutMapping("/api/v1/admin/products/{id}/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDetailResponse> updateVariant(
            @PathVariable Long id, @PathVariable Long variantId, @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(productService.updateVariant(id, variantId, request));
    }

    @DeleteMapping("/api/v1/admin/products/{id}/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDetailResponse> deleteVariant(@PathVariable Long id, @PathVariable Long variantId) {
        return ResponseEntity.ok(productService.deleteVariant(id, variantId));
    }

    @PostMapping(value = "/api/v1/admin/products/{id}/images", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDetailResponse> addImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "primary", defaultValue = "false") boolean primary) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addImage(id, file, primary));
    }

    @DeleteMapping("/api/v1/admin/products/{id}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDetailResponse> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        return ResponseEntity.ok(productService.deleteImage(id, imageId));
    }
}
