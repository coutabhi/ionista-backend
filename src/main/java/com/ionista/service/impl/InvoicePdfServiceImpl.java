package com.ionista.service.impl;

import com.ionista.entity.Order;
import com.ionista.entity.OrderItem;
import com.ionista.service.InvoicePdfService;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfServiceImpl implements InvoicePdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    @Override
    public byte[] generateInvoice(Order order) {
        String html = buildHtml(order);
        try {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            renderer.createPDF(os);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render invoice PDF for order " + order.getId(), e);
        }
    }

    private String buildHtml(Order order) {
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem item : order.getOrderItems()) {
            BigDecimal lineTotal = item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()));
            itemsHtml.append("<tr>")
                    .append("<td>").append(esc(item.getProductNameSnapshot())).append("</td>")
                    .append("<td>").append(esc(item.getSkuSnapshot())).append("</td>")
                    .append("<td>").append(esc(item.getSizeSnapshot())).append(" / ").append(esc(item.getColorSnapshot())).append("</td>")
                    .append("<td class=\"num\">").append(item.getQuantity()).append("</td>")
                    .append("<td class=\"num\">").append(formatInr(item.getPriceAtPurchase())).append("</td>")
                    .append("<td class=\"num\">").append(formatInr(lineTotal)).append("</td>")
                    .append("</tr>");
        }

        StringBuilder totalsHtml = new StringBuilder();
        totalsHtml.append(totalRow("Subtotal", order.getSubtotal(), false));
        if (order.getOfferDiscountAmount() != null && order.getOfferDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            totalsHtml.append(totalRow("Offer discount", order.getOfferDiscountAmount().negate(), false));
        }
        if (order.getCouponCode() != null) {
            totalsHtml.append(totalRow("Coupon (" + esc(order.getCouponCode()) + ")", order.getCouponDiscountAmount().negate(), false));
        }
        if (order.getLoyaltyPointsRedeemed() > 0) {
            totalsHtml.append(totalRow("Loyalty points (" + order.getLoyaltyPointsRedeemed() + ")", order.getLoyaltyDiscountAmount().negate(), false));
        }
        totalsHtml.append(totalRow("Shipping", order.getShippingFee(), false));
        totalsHtml.append(totalRow("Total", order.getTotalAmount(), true));

        return "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                + "<head><style type=\"text/css\">"
                + "body { font-family: 'Times New Roman', Georgia, serif; background-color: #f7f1e8; color: #2a2420; font-size: 12px; }"
                + ".sheet { background-color: #fffcf6; border: 1px solid #b08d57; padding: 28px; }"
                + ".wordmark { font-size: 20px; letter-spacing: 3px; color: #b08d57; margin-bottom: 4px; }"
                + ".meta { color: #5c5348; font-size: 11px; margin-bottom: 20px; }"
                + "table { width: 100%; border-collapse: collapse; margin-bottom: 18px; }"
                + "th, td { padding: 6px 8px; border-bottom: 1px solid #e3d7bd; text-align: left; }"
                + "th { color: #8a7a62; font-size: 10px; text-transform: uppercase; letter-spacing: 1px; }"
                + ".num { text-align: right; }"
                + ".totals td { border-bottom: none; }"
                + ".totals .label { color: #5c5348; }"
                + ".totals .grand td { font-weight: bold; border-top: 1px solid #b08d57; padding-top: 10px; }"
                + ".footer { margin-top: 24px; font-size: 10px; color: #8a7a62; }"
                + "</style></head>"
                + "<body><div class=\"sheet\">"
                + "<div class=\"wordmark\">IONISTA</div>"
                + "<div class=\"meta\">Invoice for Order #" + order.getId() + " · Placed " + esc(order.getPlacedAt().format(DATE_FORMAT)) + "</div>"
                + "<table><thead><tr><th>Product</th><th>SKU</th><th>Size/Color</th><th class=\"num\">Qty</th><th class=\"num\">Unit price</th><th class=\"num\">Line total</th></tr></thead>"
                + "<tbody>" + itemsHtml + "</tbody></table>"
                + "<table class=\"totals\">" + totalsHtml + "</table>"
                + "<div>"
                + "<div class=\"meta\" style=\"margin-bottom:4px;\">Shipped to</div>"
                + "<div>" + esc(order.getShipFullName()) + "<br/>"
                + esc(order.getShipLine1())
                + (order.getShipLine2() != null && !order.getShipLine2().isBlank() ? ", " + esc(order.getShipLine2()) : "")
                + ", " + esc(order.getShipCity()) + ", " + esc(order.getShipState()) + " " + esc(order.getShipPostalCode()) + "<br/>"
                + esc(order.getShipCountry()) + " · " + esc(order.getShipPhone())
                + "</div></div>"
                + "<div class=\"footer\">Ionista · Jaipur, Rajasthan</div>"
                + "</div></body></html>";
    }

    private String totalRow(String label, BigDecimal amount, boolean grand) {
        return "<tr" + (grand ? " class=\"grand\"" : "") + ">"
                + "<td class=\"label\" colspan=\"5\">" + esc(label) + "</td>"
                + "<td class=\"num\">" + formatInr(amount) + "</td>"
                + "</tr>";
    }

    private String formatInr(BigDecimal amount) {
        return "Rs. " + amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
