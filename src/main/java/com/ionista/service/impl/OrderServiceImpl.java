package com.ionista.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ionista.common.PriceUtils;
import com.ionista.common.SecurityUtils;
import com.ionista.dto.request.CheckoutRequest;
import com.ionista.dto.request.VerifyPaymentRequest;
import com.ionista.dto.response.CheckoutResponse;
import com.ionista.dto.response.CouponApplicationResult;
import com.ionista.dto.response.OrderResponse;
import com.ionista.dto.response.PageResponse;
import com.ionista.entity.*;
import com.ionista.enums.OrderStatus;
import com.ionista.enums.PaymentStatus;
import com.ionista.exception.BadRequestException;
import com.ionista.exception.ConflictException;
import com.ionista.exception.ForbiddenException;
import com.ionista.exception.PaymentVerificationException;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.mapper.OrderMapper;
import com.ionista.repository.*;
import com.ionista.service.CouponService;
import com.ionista.service.EmailService;
import com.ionista.service.InvoicePdfService;
import com.ionista.service.LoyaltyService;
import com.ionista.service.OrderService;
import com.ionista.service.PricingService;
import com.ionista.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int MAX_STOCK_RETRY_ATTEMPTS = 3;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;
    private final PricingService pricingService;
    private final RazorpayService razorpayService;
    private final StockAdjustmentService stockAdjustmentService;
    private final LoyaltyService loyaltyService;
    private final EmailService emailService;
    private final InvoicePdfService invoicePdfService;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @Value("${shipping.flat-fee}")
    private BigDecimal shippingFlatFee;

    @Override
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        User user = currentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal offerDiscount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            ProductVariant variant = item.getProductVariant();
            if (item.getQuantity() > variant.getStockQuantity()) {
                throw new ConflictException("Insufficient stock for " + variant.getProduct().getName());
            }

            BigDecimal baseUnitPrice = PriceUtils.effectiveVariantPrice(variant);
            BigDecimal offerUnitPrice = pricingService.effectiveUnitPrice(variant.getProduct(), baseUnitPrice);
            BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());

            subtotal = subtotal.add(baseUnitPrice.multiply(quantity));
            offerDiscount = offerDiscount.add(baseUnitPrice.subtract(offerUnitPrice).multiply(quantity));

            orderItems.add(OrderItem.builder()
                    .product(variant.getProduct())
                    .productVariant(variant)
                    .productNameSnapshot(variant.getProduct().getName())
                    .skuSnapshot(variant.getSku())
                    .sizeSnapshot(variant.getSize())
                    .colorSnapshot(variant.getColor())
                    .priceAtPurchase(offerUnitPrice)
                    .quantity(item.getQuantity())
                    .build());
        }

        BigDecimal postOfferTotal = subtotal.subtract(offerDiscount);

        BigDecimal couponDiscount = BigDecimal.ZERO;
        String couponCode = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            CouponApplicationResult result = couponService.validateForCheckout(request.getCouponCode(), user.getId(), postOfferTotal);
            couponDiscount = result.getDiscountAmount();
            couponCode = result.getCode();
        }

        BigDecimal afterCouponTotal = postOfferTotal.subtract(couponDiscount);
        if (afterCouponTotal.compareTo(BigDecimal.ZERO) < 0) {
            afterCouponTotal = BigDecimal.ZERO;
        }

        int loyaltyPointsToRedeem = request.getLoyaltyPointsToRedeem() != null ? request.getLoyaltyPointsToRedeem() : 0;
        BigDecimal loyaltyDiscount = BigDecimal.ZERO;
        if (loyaltyPointsToRedeem > 0) {
            loyaltyDiscount = loyaltyService.previewRedemption(user, loyaltyPointsToRedeem, afterCouponTotal);
        }

        BigDecimal totalAmount = afterCouponTotal.subtract(loyaltyDiscount).add(shippingFlatFee);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Order total must be greater than zero");
        }

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .couponCode(couponCode)
                .couponDiscountAmount(couponDiscount)
                .offerDiscountAmount(offerDiscount)
                .loyaltyPointsRedeemed(loyaltyPointsToRedeem > 0 ? loyaltyPointsToRedeem : 0)
                .loyaltyDiscountAmount(loyaltyDiscount)
                .shippingFee(shippingFlatFee)
                .totalAmount(totalAmount)
                .placedAt(LocalDateTime.now())
                .addressId(address.getId())
                .shipFullName(address.getFullName())
                .shipPhone(address.getPhone())
                .shipLine1(address.getLine1())
                .shipLine2(address.getLine2())
                .shipCity(address.getCity())
                .shipState(address.getState())
                .shipPostalCode(address.getPostalCode())
                .shipCountry(address.getCountry())
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setOrderItems(orderItems);
        Order savedOrder = orderRepository.save(order);

        String razorpayOrderId = razorpayService.createOrder(totalAmount, "INR", "order_" + savedOrder.getId());

        Payment payment = Payment.builder()
                .order(savedOrder)
                .razorpayOrderId(razorpayOrderId)
                .amount(totalAmount)
                .status(PaymentStatus.CREATED)
                .build();
        paymentRepository.save(payment);

        return CheckoutResponse.builder()
                .orderId(savedOrder.getId())
                .razorpayOrderId(razorpayOrderId)
                .razorpayKeyId(razorpayService.getKeyId())
                .amount(totalAmount)
                .currency("INR")
                .build();
    }

    @Override
    @Transactional
    public OrderResponse verifyPayment(VerifyPaymentRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        User user = currentUser();
        if (!payment.getOrder().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have access to this order");
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            return orderMapper.toResponse(reloadOrder(payment.getOrder().getId()));
        }

        boolean valid = razorpayService.verifyPaymentSignature(
                request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentVerificationException("Payment verification failed");
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        confirmOrderPayment(payment);

        return orderMapper.toResponse(reloadOrder(payment.getOrder().getId()));
    }

    @Override
    @Transactional
    public void handleRazorpayWebhook(String payload, String signatureHeader) {
        if (!razorpayService.verifyWebhookSignature(payload, signatureHeader)) {
            throw new PaymentVerificationException("Invalid webhook signature");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.error("Failed to parse Razorpay webhook payload", e);
            return;
        }

        String event = root.path("event").asText();
        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        String razorpayOrderId = paymentEntity.path("order_id").asText(null);
        if (razorpayOrderId == null) {
            return;
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (payment == null) {
            return;
        }

        payment.setRawWebhookPayload(payload);

        if ("payment.captured".equals(event)) {
            if (payment.getStatus() == PaymentStatus.PAID) {
                paymentRepository.save(payment);
                return;
            }
            payment.setRazorpayPaymentId(paymentEntity.path("id").asText(null));
            payment.setMethod(paymentEntity.path("method").asText(null));
            confirmOrderPayment(payment);
        } else if ("payment.failed".equals(event)) {
            if (payment.getStatus() != PaymentStatus.PAID) {
                payment.setStatus(PaymentStatus.FAILED);
            }
            paymentRepository.save(payment);
        } else {
            paymentRepository.save(payment);
        }
    }

    @Override
    public PageResponse<OrderResponse> myOrders(Pageable pageable) {
        User user = currentUser();
        return PageResponse.of(orderRepository.findByUserId(user.getId(), pageable).map(orderMapper::toResponse));
    }

    @Override
    public OrderResponse myOrderDetail(Long id) {
        User user = currentUser();
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelMyOrder(Long id) {
        User user = currentUser();
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ConflictException("Only pending or confirmed orders can be cancelled");
        }

        boolean stockWasDecremented = order.getStatus() == OrderStatus.CONFIRMED;
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        if (stockWasDecremented) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getProductVariant() != null) {
                    stockAdjustmentService.restoreStock(item.getProductVariant().getId(), item.getQuantity());
                }
            }
        }

        return orderMapper.toResponse(order);
    }

    @Override
    public PageResponse<OrderResponse> adminList(OrderStatus status, Pageable pageable) {
        Page<Order> page = status != null
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        return PageResponse.of(page.map(orderMapper::toResponse));
    }

    @Override
    public OrderResponse adminDetail(Long id) {
        return orderMapper.toResponse(reloadOrder(id));
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus, String trackingNumber, String trackingCarrier, String trackingUrl) {
        Order order = reloadOrder(id);
        validateTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);

        if (newStatus == OrderStatus.SHIPPED) {
            if (trackingNumber == null || trackingNumber.isBlank()) {
                throw new BadRequestException("Tracking number is required when marking an order as shipped");
            }
            order.setTrackingNumber(trackingNumber.trim());
            order.setTrackingCarrier(trackingCarrier != null && !trackingCarrier.isBlank() ? trackingCarrier.trim() : null);
            order.setTrackingUrl(trackingUrl != null && !trackingUrl.isBlank() ? trackingUrl.trim() : null);
        }

        orderRepository.save(order);

        byte[] invoicePdf = null;
        if (newStatus == OrderStatus.DELIVERED) {
            loyaltyService.earnPointsForOrder(order);
            loyaltyService.awardReferralBonusIfEligible(order.getUser(), order);
            try {
                invoicePdf = invoicePdfService.generateInvoice(order);
            } catch (Exception e) {
                log.error("Failed to generate invoice PDF for delivered order {}", order.getId(), e);
            }
        }

        try {
            emailService.sendOrderStatusEmail(order, invoicePdf);
        } catch (Exception e) {
            log.error("Failed to send order status email for order {}", order.getId(), e);
        }

        return orderMapper.toResponse(order);
    }

    private void confirmOrderPayment(Payment payment) {
        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        for (OrderItem item : order.getOrderItems()) {
            if (item.getProductVariant() != null) {
                decrementStockWithRetry(item.getProductVariant().getId(), item.getQuantity());
            }
        }

        if (order.getCouponCode() != null) {
            couponRepository.findByCode(order.getCouponCode())
                    .ifPresent(coupon -> couponService.recordRedemption(coupon.getId(), order.getUser().getId(), order.getId()));
        }

        if (order.getLoyaltyPointsRedeemed() > 0) {
            loyaltyService.applyRedemption(order.getUser().getId(), order.getLoyaltyPointsRedeemed(),
                    order.getLoyaltyDiscountAmount(), order.getId());
        }

        cartRepository.findByUserId(order.getUser().getId())
                .ifPresentOrElse(
                        cart -> {
                            log.info("Clearing cart {} for user {} after confirming order {}",
                                    cart.getId(), order.getUser().getId(), order.getId());
                            cartItemRepository.deleteByCartId(cart.getId());
                        },
                        () -> log.warn("No cart found for user {} while confirming order {} — nothing to clear",
                                order.getUser().getId(), order.getId()));

        try {
            byte[] invoicePdf = invoicePdfService.generateInvoice(order);
            emailService.sendOrderConfirmationEmail(order, invoicePdf);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email (with invoice) for order {}", order.getId(), e);
        }
    }

    private void decrementStockWithRetry(Long variantId, int quantity) {
        int attempts = 0;
        while (true) {
            try {
                stockAdjustmentService.decrementStock(variantId, quantity);
                return;
            } catch (ObjectOptimisticLockingFailureException ex) {
                attempts++;
                if (attempts >= MAX_STOCK_RETRY_ATTEMPTS) {
                    throw new ConflictException("Unable to update stock for an item, please contact support");
                }
            }
        }
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.DELIVERED || next == OrderStatus.RETURNED;
            case DELIVERED -> next == OrderStatus.RETURNED;
            case CANCELLED, RETURNED -> false;
        };
        if (!valid) {
            throw new ConflictException("Cannot transition order from " + current + " to " + next);
        }
    }

    private Order reloadOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private User currentUser() {
        return userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
