package com.ionista.entity;

import com.ionista.common.BaseEntity;
import com.ionista.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "razorpay_order_id", nullable = false, unique = true, length = 60)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 60)
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature", length = 200)
    private String razorpaySignature;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 30)
    private String method;

    @Lob
    @Column(name = "raw_webhook_payload", columnDefinition = "LONGTEXT")
    private String rawWebhookPayload;
}
