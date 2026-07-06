package com.ionista.repository;

import com.ionista.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
            "WHERE oi.order.user.id = :userId AND oi.product.id = :productId AND oi.order.status = 'DELIVERED'")
    boolean existsDeliveredOrderForUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
