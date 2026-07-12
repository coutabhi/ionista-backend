package com.ionista.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ionista.dto.request.CheckoutRequest;
import com.ionista.dto.request.VerifyPaymentRequest;
import com.ionista.dto.response.CheckoutResponse;
import com.ionista.dto.response.CouponApplicationResult;
import com.ionista.dto.response.OrderResponse;
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
import com.ionista.service.LoyaltyService;
import com.ionista.service.PricingService;
import com.ionista.service.RazorpayService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private UserRepository userRepository;
    @Mock private CouponService couponService;
    @Mock private PricingService pricingService;
    @Mock private RazorpayService razorpayService;
    @Mock private StockAdjustmentService stockAdjustmentService;
    @Mock private LoyaltyService loyaltyService;
    @Mock private EmailService emailService;
    @Mock private com.ionista.service.InvoicePdfService invoicePdfService;
    @Mock private OrderMapper orderMapper;

    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(orderService, "shippingFlatFee", BigDecimal.ZERO);

        user = User.builder().email("jane@example.com").build();
        user.setId(1L);

        var principal = org.springframework.security.core.userdetails.User
                .withUsername("jane@example.com").password("x").authorities("ROLE_USER").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        lenient().when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        lenient().when(pricingService.effectiveUnitPrice(any(Product.class), any(BigDecimal.class)))
                .thenAnswer(inv -> inv.getArgument(1));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Product buildProduct(Long id) {
        Product product = Product.builder().name("Shirt").basePrice(BigDecimal.valueOf(500)).build();
        product.setId(id);
        return product;
    }

    private ProductVariant buildVariant(Long id, int stock) {
        ProductVariant variant = ProductVariant.builder().product(buildProduct(100L)).size("M").color("Red")
                .stockQuantity(stock).sku("SKU1").build();
        variant.setId(id);
        return variant;
    }

    private Cart buildCartWithItem(int quantity, int stock) {
        Cart cart = Cart.builder().user(user).build();
        cart.setId(10L);
        ProductVariant variant = buildVariant(5L, stock);
        CartItem item = CartItem.builder().cart(cart).productVariant(variant).quantity(quantity).build();
        item.setId(50L);
        cart.getItems().add(item);
        return cart;
    }

    private Address buildAddress(Long id) {
        Address address = Address.builder().user(user).fullName("Jane").phone("123")
                .line1("Street").city("City").state("State").postalCode("100000").country("India").build();
        address.setId(id);
        return address;
    }

    // ---- checkout ----

    @Test
    void checkout_throws_whenCartMissing() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        CheckoutRequest request = CheckoutRequest.builder().addressId(1L).build();

        assertThatThrownBy(() -> orderService.checkout(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void checkout_throws_whenCartEmpty() {
        Cart emptyCart = Cart.builder().user(user).build();
        emptyCart.setId(10L);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(emptyCart));
        CheckoutRequest request = CheckoutRequest.builder().addressId(1L).build();

        assertThatThrownBy(() -> orderService.checkout(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void checkout_throws_whenAddressNotFound() {
        Cart cart = buildCartWithItem(1, 10);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        CheckoutRequest request = CheckoutRequest.builder().addressId(1L).build();

        assertThatThrownBy(() -> orderService.checkout(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void checkout_throws_whenInsufficientStock() {
        Cart cart = buildCartWithItem(5, 2);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(buildAddress(1L)));
        CheckoutRequest request = CheckoutRequest.builder().addressId(1L).build();

        assertThatThrownBy(() -> orderService.checkout(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void checkout_createsOrderAndRazorpayOrder_onHappyPath() {
        Cart cart = buildCartWithItem(2, 10);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(buildAddress(1L)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(200L);
            return o;
        });
        when(razorpayService.createOrder(any(BigDecimal.class), eq("INR"), anyString())).thenReturn("rzp_order_1");
        when(razorpayService.getKeyId()).thenReturn("rzp_key");

        CheckoutRequest request = CheckoutRequest.builder().addressId(1L).build();
        CheckoutResponse response = orderService.checkout(request);

        assertThat(response.getOrderId()).isEqualTo(200L);
        assertThat(response.getRazorpayOrderId()).isEqualTo("rzp_order_1");
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.CREATED
                && p.getRazorpayOrderId().equals("rzp_order_1")));
    }

    @Test
    void checkout_appliesCouponDiscount() {
        Cart cart = buildCartWithItem(2, 10);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(buildAddress(1L)));
        when(couponService.validateForCheckout(eq("SAVE100"), eq(1L), any(BigDecimal.class)))
                .thenReturn(CouponApplicationResult.builder().couponId(9L).code("SAVE100").discountAmount(BigDecimal.valueOf(100)).build());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(200L);
            return o;
        });
        when(razorpayService.createOrder(any(BigDecimal.class), eq("INR"), anyString())).thenReturn("rzp_order_1");
        when(razorpayService.getKeyId()).thenReturn("rzp_key");

        CheckoutRequest request = CheckoutRequest.builder().addressId(1L).couponCode("SAVE100").build();
        CheckoutResponse response = orderService.checkout(request);

        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(900));
        verify(orderRepository).save(argThat(o -> o.getCouponCode().equals("SAVE100")
                && o.getCouponDiscountAmount().compareTo(BigDecimal.valueOf(100)) == 0));
    }

    @Test
    void checkout_appliesLoyaltyDiscount() {
        Cart cart = buildCartWithItem(2, 10);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(buildAddress(1L)));
        when(loyaltyService.previewRedemption(eq(user), eq(50), any(BigDecimal.class))).thenReturn(BigDecimal.valueOf(50));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(200L);
            return o;
        });
        when(razorpayService.createOrder(any(BigDecimal.class), eq("INR"), anyString())).thenReturn("rzp_order_1");
        when(razorpayService.getKeyId()).thenReturn("rzp_key");

        CheckoutRequest request = CheckoutRequest.builder().addressId(1L).loyaltyPointsToRedeem(50).build();
        CheckoutResponse response = orderService.checkout(request);

        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(950));
    }

    @Test
    void checkout_throws_whenTotalIsZeroOrNegative() {
        Cart cart = buildCartWithItem(2, 10);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(buildAddress(1L)));
        when(loyaltyService.previewRedemption(eq(user), eq(1000), any(BigDecimal.class))).thenReturn(BigDecimal.valueOf(1000));

        CheckoutRequest request = CheckoutRequest.builder().addressId(1L).loyaltyPointsToRedeem(1000).build();

        assertThatThrownBy(() -> orderService.checkout(request))
                .isInstanceOf(BadRequestException.class);
    }

    // ---- verifyPayment ----

    private Order buildOrderWithPayment(OrderStatus orderStatus, PaymentStatus paymentStatus) {
        Order order = Order.builder().user(user).status(orderStatus)
                .subtotal(BigDecimal.valueOf(1000)).totalAmount(BigDecimal.valueOf(1000))
                .couponDiscountAmount(BigDecimal.ZERO).offerDiscountAmount(BigDecimal.ZERO)
                .loyaltyDiscountAmount(BigDecimal.ZERO).loyaltyPointsRedeemed(0)
                .orderItems(List.of()).build();
        order.setId(200L);

        Payment payment = Payment.builder().order(order).razorpayOrderId("rzp_order_1")
                .amount(BigDecimal.valueOf(1000)).status(paymentStatus).build();
        order.setPayment(payment);
        return order;
    }

    @Test
    void verifyPayment_returnsCachedResponse_whenAlreadyPaid() {
        Order order = buildOrderWithPayment(OrderStatus.CONFIRMED, PaymentStatus.PAID);
        Payment payment = order.getPayment();
        when(paymentRepository.findByRazorpayOrderId("rzp_order_1")).thenReturn(Optional.of(payment));
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(200L).build());

        VerifyPaymentRequest request = VerifyPaymentRequest.builder()
                .razorpayOrderId("rzp_order_1").razorpayPaymentId("pay_1").razorpaySignature("sig").build();

        OrderResponse response = orderService.verifyPayment(request);

        assertThat(response.getId()).isEqualTo(200L);
        verify(razorpayService, never()).verifyPaymentSignature(anyString(), anyString(), anyString());
    }

    @Test
    void verifyPayment_throws_whenOrderBelongsToDifferentUser() {
        Order order = buildOrderWithPayment(OrderStatus.PENDING, PaymentStatus.CREATED);
        User otherUser = User.builder().email("other@example.com").build();
        otherUser.setId(2L);
        order.setUser(otherUser);
        Payment payment = order.getPayment();

        when(paymentRepository.findByRazorpayOrderId("rzp_order_1")).thenReturn(Optional.of(payment));

        VerifyPaymentRequest request = VerifyPaymentRequest.builder()
                .razorpayOrderId("rzp_order_1").razorpayPaymentId("pay_1").razorpaySignature("sig").build();

        assertThatThrownBy(() -> orderService.verifyPayment(request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void verifyPayment_marksFailed_whenSignatureInvalid() {
        Order order = buildOrderWithPayment(OrderStatus.PENDING, PaymentStatus.CREATED);
        Payment payment = order.getPayment();

        when(paymentRepository.findByRazorpayOrderId("rzp_order_1")).thenReturn(Optional.of(payment));
        when(razorpayService.verifyPaymentSignature("rzp_order_1", "pay_1", "bad-sig")).thenReturn(false);

        VerifyPaymentRequest request = VerifyPaymentRequest.builder()
                .razorpayOrderId("rzp_order_1").razorpayPaymentId("pay_1").razorpaySignature("bad-sig").build();

        assertThatThrownBy(() -> orderService.verifyPayment(request))
                .isInstanceOf(PaymentVerificationException.class);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void verifyPayment_confirmsOrder_onValidSignature() {
        Order order = buildOrderWithPayment(OrderStatus.PENDING, PaymentStatus.CREATED);
        Payment payment = order.getPayment();

        when(paymentRepository.findByRazorpayOrderId("rzp_order_1")).thenReturn(Optional.of(payment));
        when(razorpayService.verifyPaymentSignature("rzp_order_1", "pay_1", "good-sig")).thenReturn(true);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(200L).build());

        VerifyPaymentRequest request = VerifyPaymentRequest.builder()
                .razorpayOrderId("rzp_order_1").razorpayPaymentId("pay_1").razorpaySignature("good-sig").build();

        OrderResponse response = orderService.verifyPayment(request);

        assertThat(response.getId()).isEqualTo(200L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void verifyPayment_clearsCartItems_whenCartHasItems() {
        Order order = buildOrderWithPayment(OrderStatus.PENDING, PaymentStatus.CREATED);
        Payment payment = order.getPayment();
        Cart cart = buildCartWithItem(2, 10);

        when(paymentRepository.findByRazorpayOrderId("rzp_order_1")).thenReturn(Optional.of(payment));
        when(razorpayService.verifyPaymentSignature("rzp_order_1", "pay_1", "good-sig")).thenReturn(true);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(200L).build());

        VerifyPaymentRequest request = VerifyPaymentRequest.builder()
                .razorpayOrderId("rzp_order_1").razorpayPaymentId("pay_1").razorpaySignature("good-sig").build();

        orderService.verifyPayment(request);

        verify(cartItemRepository).deleteByCartId(10L);
    }

    // ---- handleRazorpayWebhook ----

    @Test
    void handleRazorpayWebhook_throws_whenSignatureInvalid() {
        when(razorpayService.verifyWebhookSignature("payload", "sig")).thenReturn(false);

        assertThatThrownBy(() -> orderService.handleRazorpayWebhook("payload", "sig"))
                .isInstanceOf(PaymentVerificationException.class);
    }

    @Test
    void handleRazorpayWebhook_silentlyIgnores_malformedPayload() {
        when(razorpayService.verifyWebhookSignature(anyString(), eq("sig"))).thenReturn(true);

        orderService.handleRazorpayWebhook("not-json", "sig");

        verify(paymentRepository, never()).findByRazorpayOrderId(anyString());
    }

    @Test
    void handleRazorpayWebhook_ignoresUnknownOrder() {
        String payload = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"order_id\":\"unknown_order\"}}}}";
        when(razorpayService.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(paymentRepository.findByRazorpayOrderId("unknown_order")).thenReturn(Optional.empty());

        orderService.handleRazorpayWebhook(payload, "sig");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handleRazorpayWebhook_confirmsOrder_onPaymentCaptured() {
        Order order = buildOrderWithPayment(OrderStatus.PENDING, PaymentStatus.CREATED);
        Payment payment = order.getPayment();
        String payload = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"order_id\":\"rzp_order_1\",\"id\":\"pay_1\",\"method\":\"card\"}}}}";

        when(razorpayService.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(paymentRepository.findByRazorpayOrderId("rzp_order_1")).thenReturn(Optional.of(payment));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        orderService.handleRazorpayWebhook(payload, "sig");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getRazorpayPaymentId()).isEqualTo("pay_1");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void handleRazorpayWebhook_clearsCartItems_whenCartHasItems() {
        Order order = buildOrderWithPayment(OrderStatus.PENDING, PaymentStatus.CREATED);
        Payment payment = order.getPayment();
        Cart cart = buildCartWithItem(2, 10);
        String payload = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"order_id\":\"rzp_order_1\",\"id\":\"pay_1\",\"method\":\"card\"}}}}";

        when(razorpayService.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(paymentRepository.findByRazorpayOrderId("rzp_order_1")).thenReturn(Optional.of(payment));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        orderService.handleRazorpayWebhook(payload, "sig");

        verify(cartItemRepository).deleteByCartId(10L);
    }

    @Test
    void handleRazorpayWebhook_isIdempotent_whenAlreadyPaid() {
        Order order = buildOrderWithPayment(OrderStatus.CONFIRMED, PaymentStatus.PAID);
        Payment payment = order.getPayment();
        String payload = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"order_id\":\"rzp_order_1\"}}}}";

        when(razorpayService.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(paymentRepository.findByRazorpayOrderId("rzp_order_1")).thenReturn(Optional.of(payment));

        orderService.handleRazorpayWebhook(payload, "sig");

        verify(paymentRepository).save(payment);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleRazorpayWebhook_marksFailed_onPaymentFailedEvent() {
        Order order = buildOrderWithPayment(OrderStatus.PENDING, PaymentStatus.CREATED);
        Payment payment = order.getPayment();
        String payload = "{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"order_id\":\"rzp_order_1\"}}}}";

        when(razorpayService.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(paymentRepository.findByRazorpayOrderId("rzp_order_1")).thenReturn(Optional.of(payment));

        orderService.handleRazorpayWebhook(payload, "sig");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
    }

    // ---- myOrders / myOrderDetail / cancelMyOrder ----

    @Test
    void myOrders_returnsPagedOrders() {
        Order order = buildOrderWithPayment(OrderStatus.CONFIRMED, PaymentStatus.PAID);
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findByUserId(1L, PageRequest.of(0, 10))).thenReturn(page);
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(200L).build());

        var result = orderService.myOrders(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void myOrderDetail_throws_whenNotFound() {
        when(orderRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.myOrderDetail(200L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelMyOrder_throws_whenOrderNotCancellable() {
        Order order = buildOrderWithPayment(OrderStatus.SHIPPED, PaymentStatus.PAID);
        when(orderRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelMyOrder(200L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancelMyOrder_doesNotRestoreStock_whenPending() {
        Order order = buildOrderWithPayment(OrderStatus.PENDING, PaymentStatus.CREATED);
        when(orderRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(200L).build());

        orderService.cancelMyOrder(200L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(stockAdjustmentService, never()).restoreStock(any(), anyInt());
    }

    @Test
    void cancelMyOrder_restoresStock_whenConfirmed() {
        Order order = buildOrderWithPayment(OrderStatus.CONFIRMED, PaymentStatus.PAID);
        ProductVariant variant = buildVariant(5L, 10);
        OrderItem item = OrderItem.builder().order(order).productVariant(variant).quantity(2)
                .productNameSnapshot("Shirt").priceAtPurchase(BigDecimal.valueOf(500)).build();
        order.setOrderItems(List.of(item));

        when(orderRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(200L).build());

        orderService.cancelMyOrder(200L);

        verify(stockAdjustmentService).restoreStock(5L, 2);
    }

    // ---- admin: adminList / adminDetail / updateStatus ----

    @Test
    void adminList_filtersByStatus_whenProvided() {
        Order order = buildOrderWithPayment(OrderStatus.CONFIRMED, PaymentStatus.PAID);
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findByStatus(OrderStatus.CONFIRMED, PageRequest.of(0, 10))).thenReturn(page);
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(200L).build());

        var result = orderService.adminList(OrderStatus.CONFIRMED, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository, never()).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void updateStatus_throws_onInvalidTransition() {
        Order order = buildOrderWithPayment(OrderStatus.PENDING, PaymentStatus.CREATED);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(200L, OrderStatus.DELIVERED, null, null, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_doesNotTriggerLoyalty_forNonDeliveredTransition() {
        Order order = buildOrderWithPayment(OrderStatus.PENDING, PaymentStatus.CREATED);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(200L).build());

        orderService.updateStatus(200L, OrderStatus.CONFIRMED, null, null, null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(loyaltyService, never()).earnPointsForOrder(any());
    }

    @Test
    void updateStatus_triggersLoyaltyAndReferral_onDeliveredTransition() {
        Order order = buildOrderWithPayment(OrderStatus.SHIPPED, PaymentStatus.PAID);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(200L).build());

        orderService.updateStatus(200L, OrderStatus.DELIVERED, null, null, null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(loyaltyService).earnPointsForOrder(order);
        verify(loyaltyService).awardReferralBonusIfEligible(user, order);
    }

    @Test
    void updateStatus_throws_whenShippedWithoutTrackingNumber() {
        Order order = buildOrderWithPayment(OrderStatus.CONFIRMED, PaymentStatus.PAID);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(200L, OrderStatus.SHIPPED, null, null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateStatus_throws_whenShippedWithBlankTrackingNumber() {
        Order order = buildOrderWithPayment(OrderStatus.CONFIRMED, PaymentStatus.PAID);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(200L, OrderStatus.SHIPPED, "   ", null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateStatus_setsTrackingFields_onShippedTransition() {
        Order order = buildOrderWithPayment(OrderStatus.CONFIRMED, PaymentStatus.PAID);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(200L).build());

        orderService.updateStatus(200L, OrderStatus.SHIPPED, "TRK123", "BlueDart", "https://bluedart.com/track/TRK123");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getTrackingNumber()).isEqualTo("TRK123");
        assertThat(order.getTrackingCarrier()).isEqualTo("BlueDart");
        assertThat(order.getTrackingUrl()).isEqualTo("https://bluedart.com/track/TRK123");
    }

    @Test
    void updateStatus_doesNotRequireTracking_forNonShippedTransitions() {
        Order order = buildOrderWithPayment(OrderStatus.PENDING, PaymentStatus.CREATED);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(200L).build());

        orderService.updateStatus(200L, OrderStatus.CONFIRMED, null, null, null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getTrackingNumber()).isNull();
    }
}
