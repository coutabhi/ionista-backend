package com.ionista.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AnalyticsRepository {

    private static final String PAID_STATUSES = "('CONFIRMED','SHIPPED','DELIVERED')";

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public BigDecimal sumPaidOrderTotals(LocalDateTime from, LocalDateTime to) {
        Object result = entityManager.createNativeQuery(
                        "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status IN " + PAID_STATUSES +
                                " AND placed_at BETWEEN :from AND :to")
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return new BigDecimal(result.toString());
    }

    public long countPaidOrders(LocalDateTime from, LocalDateTime to) {
        Object result = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM orders WHERE status IN " + PAID_STATUSES +
                                " AND placed_at BETWEEN :from AND :to")
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Tuple> revenueGroupedByDay(LocalDateTime from, LocalDateTime to) {
        return entityManager.createNativeQuery(
                        "SELECT DATE(placed_at) AS period, SUM(total_amount) AS amount FROM orders " +
                                "WHERE status IN " + PAID_STATUSES + " AND placed_at BETWEEN :from AND :to " +
                                "GROUP BY period ORDER BY period", Tuple.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Tuple> revenueGroupedByMonth(LocalDateTime from, LocalDateTime to) {
        return entityManager.createNativeQuery(
                        "SELECT DATE_FORMAT(placed_at, '%Y-%m') AS period, SUM(total_amount) AS amount FROM orders " +
                                "WHERE status IN " + PAID_STATUSES + " AND placed_at BETWEEN :from AND :to " +
                                "GROUP BY period ORDER BY period", Tuple.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Tuple> countOrdersByStatus() {
        return entityManager.createNativeQuery(
                        "SELECT status, COUNT(*) AS total FROM orders GROUP BY status", Tuple.class)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Tuple> topSellingProducts(LocalDateTime from, LocalDateTime to, int limit) {
        return entityManager.createNativeQuery(
                        "SELECT oi.product_id AS productId, p.name AS name, SUM(oi.quantity) AS units, " +
                                "SUM(oi.price_at_purchase * oi.quantity) AS revenue " +
                                "FROM order_items oi " +
                                "JOIN orders o ON oi.order_id = o.id " +
                                "JOIN products p ON oi.product_id = p.id " +
                                "WHERE o.status IN " + PAID_STATUSES + " AND o.placed_at BETWEEN :from AND :to " +
                                "GROUP BY oi.product_id, p.name " +
                                "ORDER BY units DESC " +
                                "LIMIT :limit", Tuple.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("limit", limit)
                .getResultList();
    }

    public long countActiveUsers() {
        Object result = entityManager.createNativeQuery("SELECT COUNT(*) FROM users WHERE is_active = true")
                .getSingleResult();
        return ((Number) result).longValue();
    }

    public long countNewSignups(LocalDateTime from, LocalDateTime to) {
        Object result = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM users WHERE created_at BETWEEN :from AND :to")
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return ((Number) result).longValue();
    }
}
