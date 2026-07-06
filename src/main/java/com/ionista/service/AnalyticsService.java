package com.ionista.service;

import com.ionista.dto.response.*;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsService {

    SalesSummaryResponse salesSummary(LocalDate from, LocalDate to);

    List<RevenueBucketResponse> revenue(String groupBy, LocalDate from, LocalDate to);

    List<OrderStatusCountResponse> orderStatusCounts();

    List<TopProductResponse> topSellingProducts(LocalDate from, LocalDate to, int limit);

    UserStatsResponse userStats(LocalDate from, LocalDate to);
}
