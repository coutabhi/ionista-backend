package com.ionista.service;

import com.ionista.dto.response.LoyaltyBalanceResponse;
import com.ionista.dto.response.LoyaltyTransactionResponse;
import com.ionista.dto.response.PageResponse;
import com.ionista.entity.Order;
import com.ionista.entity.User;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface LoyaltyService {

    BigDecimal previewRedemption(User user, int pointsToRedeem, BigDecimal maxDiscountCap);

    void applyRedemption(Long userId, int points, BigDecimal discountAmount, Long orderId);

    void earnPointsForOrder(Order order);

    void awardReferralBonusIfEligible(User user, Order order);

    LoyaltyBalanceResponse getBalance(User user);

    PageResponse<LoyaltyTransactionResponse> history(User user, Pageable pageable);
}
