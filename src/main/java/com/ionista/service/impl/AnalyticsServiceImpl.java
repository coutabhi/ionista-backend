package com.ionista.service.impl;

import com.ionista.dto.response.*;
import com.ionista.exception.BadRequestException;
import com.ionista.repository.AnalyticsRepository;
import com.ionista.service.AnalyticsService;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final LocalDate DEFAULT_FROM = LocalDate.of(2000, 1, 1);

    private final AnalyticsRepository analyticsRepository;

    @Override
    public SalesSummaryResponse salesSummary(LocalDate from, LocalDate to) {
        LocalDate rangeFrom = from != null ? from : DEFAULT_FROM;
        LocalDate rangeTo = to != null ? to : LocalDate.now();

        BigDecimal totalSales = analyticsRepository.sumPaidOrderTotals(startOf(rangeFrom), endOf(rangeTo));
        long orderCount = analyticsRepository.countPaidOrders(startOf(rangeFrom), endOf(rangeTo));

        return SalesSummaryResponse.builder()
                .totalSales(totalSales)
                .orderCount(orderCount)
                .from(rangeFrom)
                .to(rangeTo)
                .build();
    }

    @Override
    public List<RevenueBucketResponse> revenue(String groupBy, LocalDate from, LocalDate to) {
        LocalDate rangeFrom = from != null ? from : DEFAULT_FROM;
        LocalDate rangeTo = to != null ? to : LocalDate.now();

        List<Tuple> rows = switch (groupBy == null ? "day" : groupBy.toLowerCase()) {
            case "month" -> analyticsRepository.revenueGroupedByMonth(startOf(rangeFrom), endOf(rangeTo));
            case "day" -> analyticsRepository.revenueGroupedByDay(startOf(rangeFrom), endOf(rangeTo));
            default -> throw new BadRequestException("groupBy must be either 'day' or 'month'");
        };

        return rows.stream()
                .map(row -> RevenueBucketResponse.builder()
                        .period(String.valueOf(row.get("period")))
                        .amount(new BigDecimal(row.get("amount").toString()))
                        .build())
                .toList();
    }

    @Override
    public List<OrderStatusCountResponse> orderStatusCounts() {
        return analyticsRepository.countOrdersByStatus().stream()
                .map(row -> OrderStatusCountResponse.builder()
                        .status(String.valueOf(row.get("status")))
                        .count(((Number) row.get("total")).longValue())
                        .build())
                .toList();
    }

    @Override
    public List<TopProductResponse> topSellingProducts(LocalDate from, LocalDate to, int limit) {
        LocalDate rangeFrom = from != null ? from : DEFAULT_FROM;
        LocalDate rangeTo = to != null ? to : LocalDate.now();

        return analyticsRepository.topSellingProducts(startOf(rangeFrom), endOf(rangeTo), limit).stream()
                .map(row -> TopProductResponse.builder()
                        .productId(((Number) row.get("productId")).longValue())
                        .name(String.valueOf(row.get("name")))
                        .unitsSold(((Number) row.get("units")).longValue())
                        .revenue(new BigDecimal(row.get("revenue").toString()))
                        .build())
                .toList();
    }

    @Override
    public UserStatsResponse userStats(LocalDate from, LocalDate to) {
        LocalDate rangeFrom = from != null ? from : DEFAULT_FROM;
        LocalDate rangeTo = to != null ? to : LocalDate.now();

        return UserStatsResponse.builder()
                .activeUsers(analyticsRepository.countActiveUsers())
                .newSignups(analyticsRepository.countNewSignups(startOf(rangeFrom), endOf(rangeTo)))
                .build();
    }

    private LocalDateTime startOf(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime endOf(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }
}
