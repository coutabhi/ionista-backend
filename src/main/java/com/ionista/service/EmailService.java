package com.ionista.service;

import com.ionista.dto.response.EmailSendResult;
import com.ionista.entity.Order;
import com.ionista.entity.User;

import java.util.List;

public interface EmailService {

    void sendWelcomeEmail(User user);

    void sendOrderConfirmationEmail(Order order, byte[] invoicePdf);

    void sendOrderStatusEmail(Order order, byte[] invoicePdf);

    EmailSendResult sendBulk(List<String> recipientEmails, String subject, String bodyHtml);
}
