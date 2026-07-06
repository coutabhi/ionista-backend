package com.ionista.service;

import com.ionista.dto.request.ProductRequest;
import com.ionista.dto.request.ProductVariantRequest;
import com.ionista.dto.response.PageResponse;
import com.ionista.dto.response.ProductDetailResponse;
import com.ionista.dto.response.ProductSummaryResponse;
import com.ionista.enums.Gender;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public interface ProductService {

    PageResponse<ProductSummaryResponse> list(Long categoryId, Gender gender, String brand,
                                               BigDecimal minPrice, BigDecimal maxPrice,
                                               String size, String color, String keyword,
                                               Pageable pageable);

    PageResponse<ProductSummaryResponse> listByCategory(Long categoryId, Pageable pageable);

    ProductDetailResponse getById(Long id);

    ProductDetailResponse getBySlug(String slug);

    ProductDetailResponse create(ProductRequest request);

    ProductDetailResponse update(Long id, ProductRequest request);

    void delete(Long id);

    ProductDetailResponse addVariant(Long productId, ProductVariantRequest request);

    ProductDetailResponse updateVariant(Long productId, Long variantId, ProductVariantRequest request);

    ProductDetailResponse deleteVariant(Long productId, Long variantId);

    ProductDetailResponse addImage(Long productId, MultipartFile file, boolean primary);

    ProductDetailResponse deleteImage(Long productId, Long imageId);
}
