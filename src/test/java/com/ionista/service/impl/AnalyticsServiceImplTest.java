package com.ionista.service.impl;

import com.ionista.dto.response.*;
import com.ionista.exception.BadRequestException;
import com.ionista.repository.AnalyticsRepository;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    void salesSummary_returnsTotalsForGivenRange() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        when(analyticsRepository.sumPaidOrderTotals(any(), any())).thenReturn(BigDecimal.valueOf(5000));
        when(analyticsRepository.countPaidOrders(any(), any())).thenReturn(10L);

        SalesSummaryResponse response = analyticsService.salesSummary(from, to);

        assertThat(response.getTotalSales()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(response.getOrderCount()).isEqualTo(10L);
        assertThat(response.getFrom()).isEqualTo(from);
        assertThat(response.getTo()).isEqualTo(to);
    }

    @Test
    void salesSummary_usesDefaultRange_whenDatesNotProvided() {
        when(analyticsRepository.sumPaidOrderTotals(any(), any())).thenReturn(BigDecimal.ZERO);
        when(analyticsRepository.countPaidOrders(any(), any())).thenReturn(0L);

        SalesSummaryResponse response = analyticsService.salesSummary(null, null);

        assertThat(response.getFrom()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(response.getTo()).isEqualTo(LocalDate.now());
    }

    private Tuple mockTuple(String period, BigDecimal amount) {
        Tuple tuple = mock(Tuple.class);
        when(tuple.get("period")).thenReturn(period);
        when(tuple.get("amount")).thenReturn(amount);
        return tuple;
    }

    @Test
    void revenue_groupsByDay_whenGroupByIsDay() {
        Tuple tuple = mockTuple("2026-01-01", BigDecimal.valueOf(1000));
        when(analyticsRepository.revenueGroupedByDay(any(), any())).thenReturn(List.of(tuple));

        List<RevenueBucketResponse> result = analyticsService.revenue("day", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPeriod()).isEqualTo("2026-01-01");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        verify(analyticsRepository, never()).revenueGroupedByMonth(any(), any());
    }

    @Test
    void revenue_groupsByMonth_whenGroupByIsMonth() {
        Tuple tuple = mockTuple("2026-01", BigDecimal.valueOf(5000));
        when(analyticsRepository.revenueGroupedByMonth(any(), any())).thenReturn(List.of(tuple));

        List<RevenueBucketResponse> result = analyticsService.revenue("month", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPeriod()).isEqualTo("2026-01");
    }

    @Test
    void revenue_defaultsToDay_whenGroupByNull() {
        Tuple tuple = mockTuple("2026-01-01", BigDecimal.valueOf(1000));
        when(analyticsRepository.revenueGroupedByDay(any(), any())).thenReturn(List.of(tuple));

        analyticsService.revenue(null, null, null);

        verify(analyticsRepository).revenueGroupedByDay(any(), any());
    }

    @Test
    void revenue_throws_whenGroupByIsInvalid() {
        assertThatThrownBy(() -> analyticsService.revenue("year", null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void orderStatusCounts_mapsRowsToResponses() {
        Tuple tuple = mock(Tuple.class);
        when(tuple.get("status")).thenReturn("DELIVERED");
        when(tuple.get("total")).thenReturn(5L);
        when(analyticsRepository.countOrdersByStatus()).thenReturn(List.of(tuple));

        List<OrderStatusCountResponse> result = analyticsService.orderStatusCounts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("DELIVERED");
        assertThat(result.get(0).getCount()).isEqualTo(5L);
    }

    @Test
    void topSellingProducts_mapsRowsToResponses() {
        Tuple tuple = mock(Tuple.class);
        when(tuple.get("productId")).thenReturn(7L);
        when(tuple.get("name")).thenReturn("Shirt");
        when(tuple.get("units")).thenReturn(20L);
        when(tuple.get("revenue")).thenReturn(BigDecimal.valueOf(10000));
        when(analyticsRepository.topSellingProducts(any(), any(), eq(5))).thenReturn(List.of(tuple));

        List<TopProductResponse> result = analyticsService.topSellingProducts(null, null, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(7L);
        assertThat(result.get(0).getName()).isEqualTo("Shirt");
        assertThat(result.get(0).getUnitsSold()).isEqualTo(20L);
        assertThat(result.get(0).getRevenue()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    void userStats_returnsActiveAndNewSignupCounts() {
        when(analyticsRepository.countActiveUsers()).thenReturn(100L);
        when(analyticsRepository.countNewSignups(any(), any())).thenReturn(15L);

        UserStatsResponse result = analyticsService.userStats(null, null);

        assertThat(result.getActiveUsers()).isEqualTo(100L);
        assertThat(result.getNewSignups()).isEqualTo(15L);
    }
}
