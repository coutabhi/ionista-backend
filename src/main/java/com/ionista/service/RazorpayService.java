package com.ionista.service;

import java.math.BigDecimal;

public interface RazorpayService {

    String createOrder(BigDecimal amount, String currency, String receipt);

    boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature);

    boolean verifyWebhookSignature(String payload, String signatureHeader);

    String getKeyId();
}
