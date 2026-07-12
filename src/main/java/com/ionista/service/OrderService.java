package com.ionista.service;

import com.ionista.dto.request.CheckoutRequest;
import com.ionista.dto.request.VerifyPaymentRequest;
import com.ionista.dto.response.CheckoutResponse;
import com.ionista.dto.response.OrderResponse;
import com.ionista.dto.response.PageResponse;
import com.ionista.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    CheckoutResponse checkout(CheckoutRequest request);

    OrderResponse verifyPayment(VerifyPaymentRequest request);

    void handleRazorpayWebhook(String payload, String signatureHeader);

    PageResponse<OrderResponse> myOrders(Pageable pageable);

    OrderResponse myOrderDetail(Long id);

    OrderResponse cancelMyOrder(Long id);

    PageResponse<OrderResponse> adminList(OrderStatus status, Pageable pageable);

    OrderResponse adminDetail(Long id);

    OrderResponse updateStatus(Long id, OrderStatus newStatus, String trackingNumber, String trackingCarrier, String trackingUrl);
}
