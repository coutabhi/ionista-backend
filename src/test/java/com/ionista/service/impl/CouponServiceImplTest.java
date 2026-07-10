package com.ionista.service.impl;

import com.ionista.dto.request.CouponRequest;
import com.ionista.dto.response.CartResponse;
import com.ionista.dto.response.CouponApplicationResult;
import com.ionista.dto.response.CouponValidationResponse;
import com.ionista.entity.Coupon;
import com.ionista.entity.User;
import com.ionista.enums.DiscountType;
import com.ionista.exception.ConflictException;
import com.ionista.exception.CouponInvalidException;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.CouponRedemptionRepository;
import com.ionista.repository.CouponRepository;
import com.ionista.repository.UserRepository;
import com.ionista.service.CartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CartService cartService;

    @InjectMocks
    private CouponServiceImpl couponService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().email("jane@example.com").build();
        user.setId(1L);

        var principal = org.springframework.security.core.userdetails.User
                .withUsername("jane@example.com").password("x").authorities("ROLE_USER").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Coupon buildCoupon(DiscountType type, BigDecimal value) {
        Coupon coupon = Coupon.builder()
                .code("SAVE10").discountType(type).value(value)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .active(true).usedCount(0)
                .build();
        coupon.setId(1L);
        return coupon;
    }

    @Test
    void create_throws_whenCodeAlreadyExists() {
        CouponRequest request = CouponRequest.builder().code("SAVE10").discountType(DiscountType.FLAT)
                .value(BigDecimal.TEN).startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1)).build();
        when(couponRepository.existsByCode("SAVE10")).thenReturn(true);

        assertThatThrownBy(() -> couponService.create(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_savesCoupon_whenCodeIsUnique() {
        CouponRequest request = CouponRequest.builder().code("SAVE10").discountType(DiscountType.PERCENTAGE)
                .value(BigDecimal.TEN).startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1)).build();
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = couponService.create(request);

        assertThat(result.getCode()).isEqualTo("SAVE10");
    }

    @Test
    void delete_deactivatesCoupon() {
        Coupon coupon = buildCoupon(DiscountType.FLAT, BigDecimal.TEN);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        couponService.delete(1L);

        assertThat(coupon.isActive()).isFalse();
        verify(couponRepository).save(coupon);
    }

    @Test
    void validateForCheckout_throws_whenCouponNotFound() {
        when(couponRepository.findByCode("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.validateForCheckout("MISSING", 1L, BigDecimal.valueOf(500)))
                .isInstanceOf(CouponInvalidException.class);
    }

    @Test
    void validateForCheckout_throws_whenCouponInactive() {
        Coupon coupon = buildCoupon(DiscountType.FLAT, BigDecimal.TEN);
        coupon.setActive(false);
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.validateForCheckout("SAVE10", 1L, BigDecimal.valueOf(500)))
                .isInstanceOf(CouponInvalidException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void validateForCheckout_throws_whenOutsideDateRange() {
        Coupon coupon = buildCoupon(DiscountType.FLAT, BigDecimal.TEN);
        coupon.setStartDate(LocalDateTime.now().plusDays(1));
        coupon.setEndDate(LocalDateTime.now().plusDays(2));
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.validateForCheckout("SAVE10", 1L, BigDecimal.valueOf(500)))
                .isInstanceOf(CouponInvalidException.class)
                .hasMessageContaining("not valid at this time");
    }

    @Test
    void validateForCheckout_throws_whenBelowMinOrderValue() {
        Coupon coupon = buildCoupon(DiscountType.FLAT, BigDecimal.TEN);
        coupon.setMinOrderValue(BigDecimal.valueOf(1000));
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.validateForCheckout("SAVE10", 1L, BigDecimal.valueOf(500)))
                .isInstanceOf(CouponInvalidException.class)
                .hasMessageContaining("Minimum order value");
    }

    @Test
    void validateForCheckout_throws_whenUsageLimitReached() {
        Coupon coupon = buildCoupon(DiscountType.FLAT, BigDecimal.TEN);
        coupon.setUsageLimit(5);
        coupon.setUsedCount(5);
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.validateForCheckout("SAVE10", 1L, BigDecimal.valueOf(500)))
                .isInstanceOf(CouponInvalidException.class)
                .hasMessageContaining("usage limit");
    }

    @Test
    void validateForCheckout_throws_whenPerUserLimitReached() {
        Coupon coupon = buildCoupon(DiscountType.FLAT, BigDecimal.TEN);
        coupon.setPerUserLimit(1);
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
        when(couponRedemptionRepository.countByCouponIdAndUserId(1L, 1L)).thenReturn(1L);

        assertThatThrownBy(() -> couponService.validateForCheckout("SAVE10", 1L, BigDecimal.valueOf(500)))
                .isInstanceOf(CouponInvalidException.class)
                .hasMessageContaining("maximum number of times");
    }

    @Test
    void validateForCheckout_computesPercentageDiscount() {
        Coupon coupon = buildCoupon(DiscountType.PERCENTAGE, BigDecimal.valueOf(10));
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        CouponApplicationResult result = couponService.validateForCheckout("SAVE10", 1L, BigDecimal.valueOf(1000));

        assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(100).setScale(2));
    }

    @Test
    void validateForCheckout_capsDiscountAtMaxDiscountAmount() {
        Coupon coupon = buildCoupon(DiscountType.PERCENTAGE, BigDecimal.valueOf(50));
        coupon.setMaxDiscountAmount(BigDecimal.valueOf(100));
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        CouponApplicationResult result = couponService.validateForCheckout("SAVE10", 1L, BigDecimal.valueOf(1000));

        assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void validateForCheckout_capsDiscountAtSubtotal() {
        Coupon coupon = buildCoupon(DiscountType.FLAT, BigDecimal.valueOf(5000));
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        CouponApplicationResult result = couponService.validateForCheckout("SAVE10", 1L, BigDecimal.valueOf(500));

        assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void validate_returnsValidResponse_whenCouponApplies() {
        Coupon coupon = buildCoupon(DiscountType.FLAT, BigDecimal.valueOf(50));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(cartService.getCart()).thenReturn(CartResponse.builder().subtotal(BigDecimal.valueOf(500)).build());
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        CouponValidationResponse response = couponService.validate("SAVE10");

        assertThat(response.isValid()).isTrue();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void validate_returnsInvalidResponse_whenCouponRejected() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(cartService.getCart()).thenReturn(CartResponse.builder().subtotal(BigDecimal.valueOf(500)).build());
        when(couponRepository.findByCode("BADCODE")).thenReturn(Optional.empty());

        CouponValidationResponse response = couponService.validate("BADCODE");

        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void recordRedemption_incrementsUsedCountAndSavesRedemption() {
        Coupon coupon = buildCoupon(DiscountType.FLAT, BigDecimal.TEN);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        couponService.recordRedemption(1L, 1L, 99L);

        assertThat(coupon.getUsedCount()).isEqualTo(1);
        verify(couponRepository).save(coupon);
        verify(couponRedemptionRepository).save(argThat(r -> r.getOrderId().equals(99L) && r.getUser() == user));
    }

    @Test
    void recordRedemption_throws_whenCouponNotFound() {
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.recordRedemption(1L, 1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
