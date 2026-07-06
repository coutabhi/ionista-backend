package com.ionista.service.impl;

import com.ionista.dto.response.LoyaltyBalanceResponse;
import com.ionista.dto.response.LoyaltyTransactionResponse;
import com.ionista.dto.response.PageResponse;
import com.ionista.entity.LoyaltyTransaction;
import com.ionista.entity.Order;
import com.ionista.entity.User;
import com.ionista.enums.LoyaltyTransactionType;
import com.ionista.enums.OrderStatus;
import com.ionista.exception.BadRequestException;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.LoyaltyTransactionRepository;
import com.ionista.repository.OrderRepository;
import com.ionista.repository.UserRepository;
import com.ionista.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private final UserRepository userRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final OrderRepository orderRepository;

    @Value("${loyalty.points-per-currency-unit}")
    private BigDecimal pointsPerCurrencyUnit;

    @Value("${loyalty.point-value-in-currency}")
    private BigDecimal pointValueInCurrency;

    @Value("${loyalty.referral-bonus-points}")
    private int referralBonusPoints;

    @Override
    public BigDecimal previewRedemption(User user, int pointsToRedeem, BigDecimal maxDiscountCap) {
        if (pointsToRedeem <= 0) {
            return BigDecimal.ZERO;
        }
        if (pointsToRedeem > user.getLoyaltyPoints()) {
            throw new BadRequestException("You do not have enough loyalty points");
        }

        BigDecimal discount = pointValueInCurrency.multiply(BigDecimal.valueOf(pointsToRedeem));
        return discount.min(maxDiscountCap);
    }

    @Override
    @Transactional
    public void applyRedemption(Long userId, int points, BigDecimal discountAmount, Long orderId) {
        if (points <= 0) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setLoyaltyPoints(Math.max(user.getLoyaltyPoints() - points, 0));
        userRepository.save(user);

        recordTransaction(user, -points, LoyaltyTransactionType.REDEEMED_ORDER, orderId,
                "Redeemed " + points + " points for order #" + orderId);
    }

    @Override
    @Transactional
    public void earnPointsForOrder(Order order) {
        BigDecimal points = order.getTotalAmount().divide(pointsPerCurrencyUnit, 0, RoundingMode.DOWN);
        int earned = points.intValue();
        if (earned <= 0) {
            return;
        }

        User user = order.getUser();
        user.setLoyaltyPoints(user.getLoyaltyPoints() + earned);
        userRepository.save(user);

        recordTransaction(user, earned, LoyaltyTransactionType.EARNED_ORDER, order.getId(),
                "Earned " + earned + " points for order #" + order.getId());
    }

    @Override
    @Transactional
    public void awardReferralBonusIfEligible(User user, Order order) {
        if (user.getReferredByUserId() == null) {
            return;
        }
        if (orderRepository.countByUserIdAndStatus(user.getId(), OrderStatus.DELIVERED) != 1) {
            return;
        }

        userRepository.findById(user.getReferredByUserId()).ifPresent(referrer -> {
            referrer.setLoyaltyPoints(referrer.getLoyaltyPoints() + referralBonusPoints);
            userRepository.save(referrer);
            recordTransaction(referrer, referralBonusPoints, LoyaltyTransactionType.REFERRAL_BONUS_REFERRER, order.getId(),
                    "Referral bonus for inviting a customer who completed their first order");
        });

        user.setLoyaltyPoints(user.getLoyaltyPoints() + referralBonusPoints);
        userRepository.save(user);
        recordTransaction(user, referralBonusPoints, LoyaltyTransactionType.REFERRAL_BONUS_REFEREE, order.getId(),
                "Referral bonus for your first completed order");
    }

    @Override
    public LoyaltyBalanceResponse getBalance(User user) {
        return LoyaltyBalanceResponse.builder()
                .points(user.getLoyaltyPoints())
                .referralCode(user.getReferralCode())
                .referredByUserId(user.getReferredByUserId())
                .build();
    }

    @Override
    public PageResponse<LoyaltyTransactionResponse> history(User user, Pageable pageable) {
        return PageResponse.of(loyaltyTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toResponse));
    }

    private void recordTransaction(User user, int points, LoyaltyTransactionType type, Long orderId, String description) {
        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .user(user)
                .points(points)
                .type(type)
                .relatedOrderId(orderId)
                .description(description)
                .build();
        loyaltyTransactionRepository.save(transaction);
    }

    private LoyaltyTransactionResponse toResponse(LoyaltyTransaction transaction) {
        return LoyaltyTransactionResponse.builder()
                .id(transaction.getId())
                .points(transaction.getPoints())
                .type(transaction.getType())
                .relatedOrderId(transaction.getRelatedOrderId())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
