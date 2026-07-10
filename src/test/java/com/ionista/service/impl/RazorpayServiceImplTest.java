package com.ionista.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RazorpayServiceImplTest {

    private static final String KEY_ID = "rzp_test_key";
    private static final String KEY_SECRET = "test_secret";
    private static final String WEBHOOK_SECRET = "webhook_secret";

    private RazorpayServiceImpl razorpayService;

    @BeforeEach
    void setUp() {
        razorpayService = new RazorpayServiceImpl(KEY_ID, KEY_SECRET, WEBHOOK_SECRET);
    }

    private String hmacHex(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : rawHmac) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Test
    void getKeyId_returnsConfiguredKey() {
        assertThat(razorpayService.getKeyId()).isEqualTo(KEY_ID);
    }

    @Test
    void verifyPaymentSignature_returnsTrue_forCorrectSignature() throws Exception {
        String orderId = "order_123";
        String paymentId = "pay_456";
        String signature = hmacHex(KEY_SECRET, orderId + "|" + paymentId);

        boolean result = razorpayService.verifyPaymentSignature(orderId, paymentId, signature);

        assertThat(result).isTrue();
    }

    @Test
    void verifyPaymentSignature_returnsFalse_forTamperedSignature() {
        boolean result = razorpayService.verifyPaymentSignature("order_123", "pay_456", "not-a-real-signature");

        assertThat(result).isFalse();
    }

    @Test
    void verifyWebhookSignature_returnsTrue_forCorrectSignature() throws Exception {
        String payload = "{\"event\":\"payment.captured\"}";
        String signature = hmacHex(WEBHOOK_SECRET, payload);

        boolean result = razorpayService.verifyWebhookSignature(payload, signature);

        assertThat(result).isTrue();
    }

    @Test
    void verifyWebhookSignature_returnsFalse_forTamperedPayload() throws Exception {
        String signature = hmacHex(WEBHOOK_SECRET, "{\"event\":\"payment.captured\"}");

        boolean result = razorpayService.verifyWebhookSignature("{\"event\":\"payment.failed\"}", signature);

        assertThat(result).isFalse();
    }
}
