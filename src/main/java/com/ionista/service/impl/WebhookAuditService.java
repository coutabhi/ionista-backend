package com.ionista.service.impl;

import com.ionista.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists the raw Razorpay webhook payload in its own transaction, isolated from
 * {@code OrderServiceImpl.confirmOrderPayment}'s critical work (stock, cart, loyalty, order status).
 * A failure here (e.g. an oversized payload) must never roll back that critical transaction.
 */
@Service
@RequiredArgsConstructor
public class WebhookAuditService {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRawPayload(Long paymentId, String payload) {
        paymentRepository.findById(paymentId).ifPresent(payment -> {
            payment.setRawWebhookPayload(payload);
            paymentRepository.save(payment);
        });
    }
}
