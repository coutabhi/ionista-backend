package com.ionista.service;

import com.ionista.dto.request.CouponRequest;
import com.ionista.dto.response.CouponApplicationResult;
import com.ionista.dto.response.CouponResponse;
import com.ionista.dto.response.CouponValidationResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    List<CouponResponse> listAll();

    CouponResponse create(CouponRequest request);

    CouponResponse update(Long id, CouponRequest request);

    void delete(Long id);

    CouponValidationResponse validate(String code);

    CouponApplicationResult validateForCheckout(String code, Long userId, BigDecimal subtotal);

    void recordRedemption(Long couponId, Long userId, Long orderId);
}
