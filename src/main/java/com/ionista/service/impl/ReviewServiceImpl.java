package com.ionista.service.impl;

import com.ionista.common.SecurityUtils;
import com.ionista.dto.request.ReviewRequest;
import com.ionista.dto.response.PageResponse;
import com.ionista.dto.response.ProductRatingSummaryResponse;
import com.ionista.dto.response.ReviewResponse;
import com.ionista.entity.Product;
import com.ionista.entity.Review;
import com.ionista.entity.User;
import com.ionista.exception.BadRequestException;
import com.ionista.exception.ConflictException;
import com.ionista.exception.ForbiddenException;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.OrderItemRepository;
import com.ionista.repository.ProductRepository;
import com.ionista.repository.ReviewRepository;
import com.ionista.repository.UserRepository;
import com.ionista.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Override
    public PageResponse<ReviewResponse> listByProduct(Long productId, Pageable pageable) {
        return PageResponse.of(reviewRepository.findByProductId(productId, pageable).map(this::toResponse));
    }

    @Override
    public ProductRatingSummaryResponse ratingSummary(Long productId) {
        return ProductRatingSummaryResponse.builder()
                .average(reviewRepository.averageRatingByProductId(productId))
                .count(reviewRepository.countByProductId(productId))
                .build();
    }

    @Override
    public ReviewResponse create(Long productId, ReviewRequest request) {
        User user = currentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (reviewRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new ConflictException("You have already reviewed this product");
        }

        boolean verifiedPurchase = orderItemRepository.existsDeliveredOrderForUserAndProduct(user.getId(), productId);
        if (!verifiedPurchase) {
            throw new BadRequestException("You can only review products you have purchased and received");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .verifiedPurchase(true)
                .build();

        return toResponse(reviewRepository.save(review));
    }

    @Override
    public ReviewResponse update(Long id, ReviewRequest request) {
        User user = currentUser();
        Review review = reviewRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return toResponse(reviewRepository.save(review));
    }

    @Override
    public void delete(Long id) {
        User user = currentUser();
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(user.getId()) && !SecurityUtils.isAdmin()) {
            throw new ForbiddenException("You do not have permission to delete this review");
        }

        reviewRepository.delete(review);
    }

    private User currentUser() {
        return userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .rating(review.getRating())
                .comment(review.getComment())
                .verifiedPurchase(review.isVerifiedPurchase())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
