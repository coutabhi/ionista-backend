package com.ionista.service.impl;

import com.ionista.dto.request.AddToCartRequest;
import com.ionista.dto.request.UpdateCartItemRequest;
import com.ionista.dto.response.CartResponse;
import com.ionista.entity.Cart;
import com.ionista.entity.CartItem;
import com.ionista.entity.Product;
import com.ionista.entity.ProductVariant;
import com.ionista.entity.User;
import com.ionista.exception.ConflictException;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.mapper.CartMapper;
import com.ionista.repository.CartItemRepository;
import com.ionista.repository.CartRepository;
import com.ionista.repository.ProductVariantRepository;
import com.ionista.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = User.builder().email("jane@example.com").build();
        user.setId(1L);
        cart = Cart.builder().user(user).build();
        cart.setId(10L);

        var principal = org.springframework.security.core.userdetails.User
                .withUsername("jane@example.com").password("x").authorities("ROLE_USER").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        lenient().when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        lenient().when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().id(10L).build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Product buildProduct() {
        Product product = Product.builder().name("Shirt").basePrice(BigDecimal.valueOf(500)).build();
        product.setId(1L);
        return product;
    }

    private ProductVariant buildVariant(Long id, int stock) {
        ProductVariant variant = ProductVariant.builder().product(buildProduct()).size("M").color("Red")
                .stockQuantity(stock).sku("VSKU").build();
        variant.setId(id);
        return variant;
    }

    @Test
    void getCart_createsCart_whenNoneExists() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.getCart();

        assertThat(response.getId()).isEqualTo(10L);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void getCart_reusesExistingCart() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        cartService.getCart();

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItem_createsNewCartItem_whenNotAlreadyInCart() {
        ProductVariant variant = buildVariant(5L, 10);
        AddToCartRequest request = AddToCartRequest.builder().variantId(5L).quantity(2).build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(5L)).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(10L, 5L)).thenReturn(Optional.empty());

        cartService.addItem(request);

        verify(cartItemRepository).save(argThat(item -> item.getQuantity() == 2 && item.getProductVariant() == variant));
    }

    @Test
    void addItem_incrementsQuantity_whenAlreadyInCart() {
        ProductVariant variant = buildVariant(5L, 10);
        CartItem existingItem = CartItem.builder().cart(cart).productVariant(variant).quantity(3).build();
        AddToCartRequest request = AddToCartRequest.builder().variantId(5L).quantity(2).build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(5L)).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(10L, 5L)).thenReturn(Optional.of(existingItem));

        cartService.addItem(request);

        assertThat(existingItem.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    void addItem_throws_whenRequestedQuantityExceedsStock() {
        ProductVariant variant = buildVariant(5L, 1);
        AddToCartRequest request = AddToCartRequest.builder().variantId(5L).quantity(2).build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(5L)).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(10L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(request))
                .isInstanceOf(ConflictException.class);

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_throws_whenVariantNotFound() {
        AddToCartRequest request = AddToCartRequest.builder().variantId(99L).quantity(1).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItem_updatesQuantity_whenWithinStock() {
        ProductVariant variant = buildVariant(5L, 10);
        CartItem item = CartItem.builder().cart(cart).productVariant(variant).quantity(1).build();
        item.setId(20L);
        UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(4).build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(20L, 10L)).thenReturn(Optional.of(item));

        cartService.updateItem(20L, request);

        assertThat(item.getQuantity()).isEqualTo(4);
    }

    @Test
    void updateItem_throws_whenQuantityExceedsStock() {
        ProductVariant variant = buildVariant(5L, 2);
        CartItem item = CartItem.builder().cart(cart).productVariant(variant).quantity(1).build();
        item.setId(20L);
        UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(5).build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(20L, 10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateItem(20L, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateItem_throws_whenItemNotFound() {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(1).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(20L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItem(20L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeItem_deletesItem_whenFound() {
        ProductVariant variant = buildVariant(5L, 10);
        CartItem item = CartItem.builder().cart(cart).productVariant(variant).quantity(1).build();
        item.setId(20L);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(20L, 10L)).thenReturn(Optional.of(item));

        cartService.removeItem(20L);

        verify(cartItemRepository).delete(item);
    }

    @Test
    void clearCart_deletesAllItemsForCart() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        cartService.clearCart();

        verify(cartItemRepository).deleteByCartId(10L);
    }
}
