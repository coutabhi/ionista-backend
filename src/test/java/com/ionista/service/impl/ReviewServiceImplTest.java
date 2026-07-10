package com.ionista.service.impl;

import com.ionista.dto.request.ReviewRequest;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().firstName("Jane").lastName("Doe").email("jane@example.com").build();
        user.setId(1L);
        authenticateAs("jane@example.com", "ROLE_USER");
        lenient().when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email, String role) {
        var principal = org.springframework.security.core.userdetails.User
                .withUsername(email).password("x").authorities(role).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Product buildProduct(Long id) {
        Product product = Product.builder().name("Shirt").build();
        product.setId(id);
        return product;
    }

    @Test
    void create_savesReview_whenVerifiedPurchase() {
        Product product = buildProduct(2L);
        ReviewRequest request = ReviewRequest.builder().rating(5).comment("Great!").build();

        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByUserIdAndProductId(1L, 2L)).thenReturn(false);
        when(orderItemRepository.existsDeliveredOrderForUserAndProduct(1L, 2L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = reviewService.create(2L, request);

        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.isVerifiedPurchase()).isTrue();
        assertThat(response.getUserName()).isEqualTo("Jane Doe");
    }

    @Test
    void create_throws_whenProductNotFound() {
        when(productRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create(2L, ReviewRequest.builder().rating(5).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_throws_whenAlreadyReviewed() {
        Product product = buildProduct(2L);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByUserIdAndProductId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.create(2L, ReviewRequest.builder().rating(5).build()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_throws_whenNotVerifiedPurchase() {
        Product product = buildProduct(2L);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByUserIdAndProductId(1L, 2L)).thenReturn(false);
        when(orderItemRepository.existsDeliveredOrderForUserAndProduct(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.create(2L, ReviewRequest.builder().rating(5).build()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void update_updatesOwnReview() {
        Review review = Review.builder().user(user).product(buildProduct(2L)).rating(3).comment("ok").build();
        review.setId(9L);
        when(reviewRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = reviewService.update(9L, ReviewRequest.builder().rating(4).comment("better").build());

        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getComment()).isEqualTo("better");
    }

    @Test
    void update_throws_whenReviewNotOwnedByUser() {
        when(reviewRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.update(9L, ReviewRequest.builder().rating(4).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_allowsOwner() {
        Review review = Review.builder().user(user).product(buildProduct(2L)).rating(3).build();
        review.setId(9L);
        when(reviewRepository.findById(9L)).thenReturn(Optional.of(review));

        reviewService.delete(9L);

        verify(reviewRepository).delete(review);
    }

    @Test
    void delete_allowsAdmin_forOtherUsersReview() {
        User otherUser = User.builder().firstName("Bob").lastName("Smith").email("bob@example.com").build();
        otherUser.setId(2L);
        Review review = Review.builder().user(otherUser).product(buildProduct(2L)).rating(3).build();
        review.setId(9L);

        SecurityContextHolder.clearContext();
        authenticateAs("jane@example.com", "ROLE_ADMIN");
        when(reviewRepository.findById(9L)).thenReturn(Optional.of(review));

        reviewService.delete(9L);

        verify(reviewRepository).delete(review);
    }

    @Test
    void delete_throws_whenNotOwnerAndNotAdmin() {
        User otherUser = User.builder().firstName("Bob").lastName("Smith").email("bob@example.com").build();
        otherUser.setId(2L);
        Review review = Review.builder().user(otherUser).product(buildProduct(2L)).rating(3).build();
        review.setId(9L);

        when(reviewRepository.findById(9L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.delete(9L))
                .isInstanceOf(ForbiddenException.class);

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void listByProduct_returnsMappedPage() {
        Review review = Review.builder().user(user).product(buildProduct(2L)).rating(4).build();
        review.setId(9L);
        Page<Review> page = new PageImpl<>(List.of(review));
        when(reviewRepository.findByProductId(2L, PageRequest.of(0, 10))).thenReturn(page);

        var result = reviewService.listByProduct(2L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void ratingSummary_returnsAverageAndCount() {
        when(reviewRepository.averageRatingByProductId(2L)).thenReturn(4.5);
        when(reviewRepository.countByProductId(2L)).thenReturn(10L);

        var summary = reviewService.ratingSummary(2L);

        assertThat(summary.getAverage()).isEqualTo(4.5);
        assertThat(summary.getCount()).isEqualTo(10L);
    }
}
