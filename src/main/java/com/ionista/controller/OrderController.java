package com.ionista.controller;

import com.ionista.dto.request.OrderStatusUpdateRequest;
import com.ionista.dto.response.OrderResponse;
import com.ionista.dto.response.PageResponse;
import com.ionista.enums.OrderStatus;
import com.ionista.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/api/v1/orders")
    public ResponseEntity<PageResponse<OrderResponse>> myOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.myOrders(pageable));
    }

    @GetMapping("/api/v1/orders/{id}")
    public ResponseEntity<OrderResponse> myOrderDetail(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.myOrderDetail(id));
    }

    @PostMapping("/api/v1/orders/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelMyOrder(id));
    }

    @GetMapping("/api/v1/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderResponse>> adminList(
            @RequestParam(required = false) OrderStatus status, Pageable pageable) {
        return ResponseEntity.ok(orderService.adminList(status, pageable));
    }

    @GetMapping("/api/v1/admin/orders/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> adminDetail(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.adminDetail(id));
    }

    @PatchMapping("/api/v1/admin/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.getStatus(),
                request.getTrackingNumber(), request.getTrackingCarrier(), request.getTrackingUrl()));
    }
}
