package com.ionista.service.impl;

import com.ionista.entity.Order;
import com.ionista.entity.User;
import com.ionista.enums.LoyaltyTransactionType;
import com.ionista.enums.OrderStatus;
import com.ionista.exception.BadRequestException;
import com.ionista.repository.LoyaltyTransactionRepository;
import com.ionista.repository.OrderRepository;
import com.ionista.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private LoyaltyTransactionRepository loyaltyTransactionRepository;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private LoyaltyServiceImpl loyaltyService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loyaltyService, "pointsPerCurrencyUnit", BigDecimal.valueOf(100));
        ReflectionTestUtils.setField(loyaltyService, "pointValueInCurrency", BigDecimal.valueOf(1));
        ReflectionTestUtils.setField(loyaltyService, "referralBonusPoints", 50);
    }

    private User buildUser(Long id, int points, Long referredBy) {
        User user = User.builder().email("jane@example.com").loyaltyPoints(points).referredByUserId(referredBy).build();
        user.setId(id);
        return user;
    }

    private Order buildOrder(User owner, BigDecimal totalAmount) {
        Order order = Order.builder().user(owner).status(OrderStatus.DELIVERED).totalAmount(totalAmount)
                .subtotal(totalAmount).build();
        order.setId(500L);
        return order;
    }

    @Test
    void previewRedemption_returnsZero_whenPointsNotPositive() {
        User user = buildUser(1L, 100, null);

        BigDecimal discount = loyaltyService.previewRedemption(user, 0, BigDecimal.valueOf(1000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void previewRedemption_throws_whenNotEnoughPoints() {
        User user = buildUser(1L, 10, null);

        assertThatThrownBy(() -> loyaltyService.previewRedemption(user, 50, BigDecimal.valueOf(1000)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void previewRedemption_capsAtMaxDiscount() {
        User user = buildUser(1L, 1000, null);

        BigDecimal discount = loyaltyService.previewRedemption(user, 500, BigDecimal.valueOf(100));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void previewRedemption_returnsPointValue_whenBelowCap() {
        User user = buildUser(1L, 1000, null);

        BigDecimal discount = loyaltyService.previewRedemption(user, 50, BigDecimal.valueOf(1000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void applyRedemption_doesNothing_whenPointsNotPositive() {
        loyaltyService.applyRedemption(1L, 0, BigDecimal.ZERO, 500L);

        verify(userRepository, never()).findById(any());
    }

    @Test
    void applyRedemption_deductsPointsAndRecordsTransaction() {
        User user = buildUser(1L, 100, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        loyaltyService.applyRedemption(1L, 30, BigDecimal.valueOf(30), 500L);

        assertThat(user.getLoyaltyPoints()).isEqualTo(70);
        verify(userRepository).save(user);
        verify(loyaltyTransactionRepository).save(argThat(t ->
                t.getPoints() == -30 && t.getType() == LoyaltyTransactionType.REDEEMED_ORDER));
    }

    @Test
    void applyRedemption_neverGoesNegative() {
        User user = buildUser(1L, 10, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        loyaltyService.applyRedemption(1L, 30, BigDecimal.valueOf(10), 500L);

        assertThat(user.getLoyaltyPoints()).isEqualTo(0);
    }

    @Test
    void earnPointsForOrder_creditsPointsBasedOnRate() {
        User user = buildUser(1L, 0, null);
        Order order = buildOrder(user, BigDecimal.valueOf(950));

        loyaltyService.earnPointsForOrder(order);

        assertThat(user.getLoyaltyPoints()).isEqualTo(9);
        verify(userRepository).save(user);
        verify(loyaltyTransactionRepository).save(argThat(t ->
                t.getPoints() == 9 && t.getType() == LoyaltyTransactionType.EARNED_ORDER));
    }

    @Test
    void earnPointsForOrder_doesNothing_whenBelowOnePoint() {
        User user = buildUser(1L, 0, null);
        Order order = buildOrder(user, BigDecimal.valueOf(50));

        loyaltyService.earnPointsForOrder(order);

        verify(userRepository, never()).save(any());
    }

    @Test
    void awardReferralBonusIfEligible_doesNothing_whenNoReferrer() {
        User user = buildUser(1L, 0, null);
        Order order = buildOrder(user, BigDecimal.valueOf(500));

        loyaltyService.awardReferralBonusIfEligible(user, order);

        verify(orderRepository, never()).countByUserIdAndStatus(any(), any());
    }

    @Test
    void awardReferralBonusIfEligible_doesNothing_whenNotFirstDeliveredOrder() {
        User user = buildUser(1L, 0, 99L);
        Order order = buildOrder(user, BigDecimal.valueOf(500));
        when(orderRepository.countByUserIdAndStatus(1L, OrderStatus.DELIVERED)).thenReturn(2L);

        loyaltyService.awardReferralBonusIfEligible(user, order);

        verify(userRepository, never()).findById(99L);
    }

    @Test
    void awardReferralBonusIfEligible_creditsBothReferrerAndReferee_onFirstDeliveredOrder() {
        User referee = buildUser(1L, 0, 99L);
        User referrer = buildUser(99L, 0, null);
        Order order = buildOrder(referee, BigDecimal.valueOf(500));

        when(orderRepository.countByUserIdAndStatus(1L, OrderStatus.DELIVERED)).thenReturn(1L);
        when(userRepository.findById(99L)).thenReturn(Optional.of(referrer));

        loyaltyService.awardReferralBonusIfEligible(referee, order);

        assertThat(referrer.getLoyaltyPoints()).isEqualTo(50);
        assertThat(referee.getLoyaltyPoints()).isEqualTo(50);
        verify(userRepository).save(referrer);
        verify(userRepository).save(referee);
        verify(loyaltyTransactionRepository).save(argThat(t -> t.getType() == LoyaltyTransactionType.REFERRAL_BONUS_REFERRER));
        verify(loyaltyTransactionRepository).save(argThat(t -> t.getType() == LoyaltyTransactionType.REFERRAL_BONUS_REFEREE));
    }

    @Test
    void getBalance_returnsUserLoyaltyInfo() {
        User user = buildUser(1L, 120, null);
        user.setReferralCode("ABC12345");

        var balance = loyaltyService.getBalance(user);

        assertThat(balance.getPoints()).isEqualTo(120);
        assertThat(balance.getReferralCode()).isEqualTo("ABC12345");
    }
}
