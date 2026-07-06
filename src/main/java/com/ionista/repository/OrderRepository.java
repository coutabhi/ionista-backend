package com.ionista.repository;

import com.ionista.entity.Order;
import com.ionista.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"orderItems", "payment"})
    Page<Order> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "payment"})
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"orderItems", "payment"})
    @Override
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = {"orderItems", "payment"})
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "payment"})
    @Override
    Page<Order> findAll(Pageable pageable);

    long countByUserIdAndStatus(Long userId, OrderStatus status);
}
