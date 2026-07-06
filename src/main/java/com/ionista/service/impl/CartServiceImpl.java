package com.ionista.service.impl;

import com.ionista.common.SecurityUtils;
import com.ionista.dto.request.AddToCartRequest;
import com.ionista.dto.request.UpdateCartItemRequest;
import com.ionista.dto.response.CartResponse;
import com.ionista.entity.Cart;
import com.ionista.entity.CartItem;
import com.ionista.entity.ProductVariant;
import com.ionista.entity.User;
import com.ionista.exception.ConflictException;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.mapper.CartMapper;
import com.ionista.repository.CartItemRepository;
import com.ionista.repository.CartRepository;
import com.ionista.repository.ProductVariantRepository;
import com.ionista.repository.UserRepository;
import com.ionista.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    @Override
    public CartResponse getCart() {
        return cartMapper.toResponse(getOrCreateCart());
    }

    @Override
    @Transactional
    public CartResponse addItem(AddToCartRequest request) {
        Cart cart = getOrCreateCart();
        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));

        CartItem item = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId())
                .orElse(null);

        int newQuantity = (item != null ? item.getQuantity() : 0) + request.getQuantity();
        if (newQuantity > variant.getStockQuantity()) {
            throw new ConflictException("Only " + variant.getStockQuantity() + " unit(s) available in stock");
        }

        if (item == null) {
            item = CartItem.builder()
                    .cart(cart)
                    .productVariant(variant)
                    .quantity(request.getQuantity())
                    .build();
        } else {
            item.setQuantity(newQuantity);
        }
        cartItemRepository.save(item);

        return getCart();
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart();
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (request.getQuantity() > item.getProductVariant().getStockQuantity()) {
            throw new ConflictException("Only " + item.getProductVariant().getStockQuantity() + " unit(s) available in stock");
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return getCart();
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long itemId) {
        Cart cart = getOrCreateCart();
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cartItemRepository.delete(item);
        return getCart();
    }

    @Override
    @Transactional
    public CartResponse clearCart() {
        Cart cart = getOrCreateCart();
        cartItemRepository.deleteByCartId(cart.getId());
        return getCart();
    }

    private Cart getOrCreateCart() {
        User user = userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }
}
