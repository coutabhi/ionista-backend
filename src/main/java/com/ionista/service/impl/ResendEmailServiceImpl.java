package com.ionista.service.impl;

import com.ionista.dto.response.EmailSendResult;
import com.ionista.entity.Order;
import com.ionista.entity.User;
import com.ionista.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ResendEmailServiceImpl implements EmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final int BATCH_SIZE = 100;

    private final RestClient restClient;
    private final String fromHeader;
    private final boolean configured;

    public ResendEmailServiceImpl(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from-email:onboarding@resend.dev}") String fromEmail,
            @Value("${resend.from-name:Ionista}") String fromName) {
        this.configured = apiKey != null && !apiKey.isBlank();
        this.fromHeader = fromName + " <" + fromEmail + ">";
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public void sendWelcomeEmail(User user) {
        String html = shell("Welcome to Ionista, " + user.getFirstName() + "!",
                "<p>Thank you for creating an account with us. Explore hand block-printed, gota-finished kurtis made in the workshops of Jaipur's old city.</p>");
        sendOne(user.getEmail(), "Welcome to Ionista", html);
    }

    @Override
    public void sendOrderConfirmationEmail(Order order, byte[] invoicePdf) {
        String html = shell("Your order is confirmed",
                "<p>Hi " + order.getShipFullName() + ",</p>"
                        + "<p>We've received your order <strong>#" + order.getId() + "</strong> placed on "
                        + order.getPlacedAt().format(DATE_FORMAT) + " for a total of <strong>₹" + order.getTotalAmount() + "</strong>.</p>"
                        + "<p>It will be shipped to: " + order.getShipLine1() + ", " + order.getShipCity() + ", " + order.getShipState()
                        + " " + order.getShipPostalCode() + "</p>"
                        + (invoicePdf != null ? "<p>Your invoice is attached to this email.</p>" : ""));
        sendOne(order.getUser().getEmail(), "Order Confirmed — #" + order.getId(), html, invoicePdf, invoiceFilename(order));
    }

    @Override
    public void sendOrderStatusEmail(Order order, byte[] invoicePdf) {
        String html = shell("Order #" + order.getId() + " update",
                "<p>Hi " + order.getShipFullName() + ",</p>"
                        + "<p>Your order status has changed to <strong>" + order.getStatus() + "</strong>.</p>"
                        + (order.getTrackingNumber() != null
                                ? "<p>Tracking number: <strong>" + order.getTrackingNumber() + "</strong>"
                                        + (order.getTrackingCarrier() != null ? " (" + order.getTrackingCarrier() + ")" : "")
                                        + (order.getTrackingUrl() != null
                                                ? " — <a href=\"" + order.getTrackingUrl() + "\">track your shipment</a>"
                                                : "")
                                        + "</p>"
                                : "")
                        + (invoicePdf != null ? "<p>Your invoice is attached to this email.</p>" : ""));
        sendOne(order.getUser().getEmail(), "Order #" + order.getId() + " is now " + order.getStatus(), html, invoicePdf, invoiceFilename(order));
    }

    private String invoiceFilename(Order order) {
        return "Ionista-Invoice-" + order.getId() + ".pdf";
    }

    @Override
    public EmailSendResult sendBulk(List<String> recipientEmails, String subject, String bodyHtml) {
        if (!configured) {
            log.warn("Resend API key not configured — skipping bulk send of {} email(s)", recipientEmails.size());
            return new EmailSendResult(0, recipientEmails.size());
        }

        String html = shell(subject, bodyHtml);
        int success = 0;
        int failed = 0;

        for (List<String> chunk : partition(recipientEmails, BATCH_SIZE)) {
            try {
                List<Map<String, Object>> payload = chunk.stream()
                        .map(email -> Map.<String, Object>of(
                                "from", fromHeader,
                                "to", List.of(email),
                                "subject", subject,
                                "html", html))
                        .toList();

                restClient.post().uri("/emails/batch").body(payload).retrieve().toBodilessEntity();
                success += chunk.size();
            } catch (Exception e) {
                log.error("Batch email send failed for {} recipient(s): {}", chunk.size(), e.getMessage());
                failed += chunk.size();
            }
        }

        return new EmailSendResult(success, failed);
    }

    private void sendOne(String to, String subject, String html) {
        sendOne(to, subject, html, null, null);
    }

    private void sendOne(String to, String subject, String html, byte[] attachmentBytes, String attachmentFilename) {
        if (!configured) {
            log.warn("Resend API key not configured — skipping email '{}' to {}", subject, to);
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("from", fromHeader);
            body.put("to", List.of(to));
            body.put("subject", subject);
            body.put("html", html);
            if (attachmentBytes != null && attachmentFilename != null) {
                body.put("attachments", List.of(Map.of(
                        "filename", attachmentFilename,
                        "content", Base64.getEncoder().encodeToString(attachmentBytes))));
            }
            restClient.post().uri("/emails").body(body).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
        }
    }

    private String shell(String heading, String bodyHtml) {
        return "<div style=\"font-family:Georgia,serif;background:#f7f1e8;padding:32px;\">"
                + "<div style=\"max-width:520px;margin:0 auto;background:#fffcf6;border:1px solid rgba(176,141,87,0.35);padding:36px;\">"
                + "<div style=\"font-size:22px;letter-spacing:2px;color:#b08d57;margin-bottom:24px;\">IONISTA</div>"
                + "<h2 style=\"color:#2a2420;font-weight:500;margin:0 0 16px;\">" + heading + "</h2>"
                + "<div style=\"color:#5c5348;font-size:15px;line-height:1.6;\">" + bodyHtml + "</div>"
                + "<div style=\"margin-top:32px;font-size:12px;color:#8a7a62;\">Ionista &middot; Jaipur, Rajasthan</div>"
                + "</div></div>";
    }

    private static List<List<String>> partition(List<String> list, int size) {
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return chunks;
    }
}
