package com.ionista.controller;

import com.ionista.dto.request.CheckoutRequest;
import com.ionista.dto.request.VerifyPaymentRequest;
import com.ionista.dto.response.CheckoutResponse;
import com.ionista.dto.response.OrderResponse;
import com.ionista.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(orderService.checkout(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<OrderResponse> verify(@Valid @RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(orderService.verifyPayment(request));
    }
}
