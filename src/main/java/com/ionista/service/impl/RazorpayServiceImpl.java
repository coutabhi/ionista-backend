package com.ionista.service.impl;

import com.ionista.exception.BadRequestException;
import com.ionista.service.RazorpayService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class RazorpayServiceImpl implements RazorpayService {

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    public RazorpayServiceImpl(@Value("${razorpay.key-id}") String keyId,
                                @Value("${razorpay.key-secret}") String keySecret,
                                @Value("${razorpay.webhook-secret}") String webhookSecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public String createOrder(BigDecimal amount, String currency, String receipt) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValueExact();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);

            com.razorpay.Order order = client.orders.create(orderRequest);
            return order.get("id");
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            throw new BadRequestException("Failed to initiate payment. Please try again.");
        }
    }

    @Override
    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);
            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            log.warn("Razorpay payment signature verification failed", e);
            return false;
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        try {
            return Utils.verifyWebhookSignature(payload, signatureHeader, webhookSecret);
        } catch (RazorpayException e) {
            log.warn("Razorpay webhook signature verification failed", e);
            return false;
        }
    }

    @Override
    public String getKeyId() {
        return keyId;
    }
}
