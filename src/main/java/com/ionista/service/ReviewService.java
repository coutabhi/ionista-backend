package com.ionista.service;

import com.ionista.dto.request.ReviewRequest;
import com.ionista.dto.response.PageResponse;
import com.ionista.dto.response.ProductRatingSummaryResponse;
import com.ionista.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    PageResponse<ReviewResponse> listByProduct(Long productId, Pageable pageable);

    ProductRatingSummaryResponse ratingSummary(Long productId);

    ReviewResponse create(Long productId, ReviewRequest request);

    ReviewResponse update(Long id, ReviewRequest request);

    void delete(Long id);
}
