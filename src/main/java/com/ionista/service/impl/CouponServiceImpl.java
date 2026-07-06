package com.ionista.service.impl;

import com.ionista.common.SecurityUtils;
import com.ionista.dto.request.CouponRequest;
import com.ionista.dto.response.CartResponse;
import com.ionista.dto.response.CouponApplicationResult;
import com.ionista.dto.response.CouponResponse;
import com.ionista.dto.response.CouponValidationResponse;
import com.ionista.entity.Coupon;
import com.ionista.entity.CouponRedemption;
import com.ionista.entity.User;
import com.ionista.enums.DiscountType;
import com.ionista.exception.ConflictException;
import com.ionista.exception.CouponInvalidException;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.CouponRedemptionRepository;
import com.ionista.repository.CouponRepository;
import com.ionista.repository.UserRepository;
import com.ionista.service.CartService;
import com.ionista.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final UserRepository userRepository;
    private final CartService cartService;

    @Override
    public List<CouponResponse> listAll() {
        return couponRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public CouponResponse create(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode())) {
            throw new ConflictException("A coupon with code '" + request.getCode() + "' already exists");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode())
                .discountType(request.getDiscountType())
                .value(request.getValue())
                .minOrderValue(request.getMinOrderValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimit(request.getUsageLimit())
                .perUserLimit(request.getPerUserLimit())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(request.getActive() == null || request.getActive())
                .build();

        return toResponse(couponRepository.save(coupon));
    }

    @Override
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = findCoupon(id);

        if (request.getCode() != null && !request.getCode().equals(coupon.getCode())) {
            if (couponRepository.existsByCode(request.getCode())) {
                throw new ConflictException("A coupon with code '" + request.getCode() + "' already exists");
            }
            coupon.setCode(request.getCode());
        }
        if (request.getDiscountType() != null) {
            coupon.setDiscountType(request.getDiscountType());
        }
        if (request.getValue() != null) {
            coupon.setValue(request.getValue());
        }
        coupon.setMinOrderValue(request.getMinOrderValue());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setPerUserLimit(request.getPerUserLimit());
        if (request.getStartDate() != null) {
            coupon.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            coupon.setEndDate(request.getEndDate());
        }
        if (request.getActive() != null) {
            coupon.setActive(request.getActive());
        }

        return toResponse(couponRepository.save(coupon));
    }

    @Override
    public void delete(Long id) {
        Coupon coupon = findCoupon(id);
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    @Override
    public CouponValidationResponse validate(String code) {
        User user = userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        CartResponse cart = cartService.getCart();

        try {
            CouponApplicationResult result = validateForCheckout(code, user.getId(), cart.getSubtotal());
            return CouponValidationResponse.builder()
                    .valid(true)
                    .discountAmount(result.getDiscountAmount())
                    .message("Coupon applied successfully")
                    .build();
        } catch (CouponInvalidException ex) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .discountAmount(BigDecimal.ZERO)
                    .message(ex.getMessage())
                    .build();
        }
    }

    @Override
    public CouponApplicationResult validateForCheckout(String code, Long userId, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new CouponInvalidException("Invalid coupon code"));

        if (!coupon.isActive()) {
            throw new CouponInvalidException("This coupon is not active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getEndDate())) {
            throw new CouponInvalidException("This coupon is not valid at this time");
        }

        if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new CouponInvalidException("Minimum order value of " + coupon.getMinOrderValue() + " required for this coupon");
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new CouponInvalidException("This coupon has reached its usage limit");
        }

        if (coupon.getPerUserLimit() != null) {
            long userRedemptions = couponRedemptionRepository.countByCouponIdAndUserId(coupon.getId(), userId);
            if (userRedemptions >= coupon.getPerUserLimit()) {
                throw new CouponInvalidException("You have already used this coupon the maximum number of times");
            }
        }

        BigDecimal discount = coupon.getDiscountType() == DiscountType.PERCENTAGE
                ? subtotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : coupon.getValue();

        if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        return CouponApplicationResult.builder()
                .couponId(coupon.getId())
                .code(coupon.getCode())
                .discountAmount(discount)
                .build();
    }

    @Override
    public void recordRedemption(Long couponId, Long userId, Long orderId) {
        Coupon coupon = findCoupon(couponId);
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CouponRedemption redemption = CouponRedemption.builder()
                .coupon(coupon)
                .user(user)
                .orderId(orderId)
                .build();
        couponRedemptionRepository.save(redemption);
    }

    private Coupon findCoupon(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .value(coupon.getValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .usageLimit(coupon.getUsageLimit())
                .perUserLimit(coupon.getPerUserLimit())
                .usedCount(coupon.getUsedCount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .active(coupon.isActive())
                .build();
    }
}
