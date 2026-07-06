package com.ionista.service;

import com.ionista.dto.request.AddToCartRequest;
import com.ionista.dto.request.UpdateCartItemRequest;
import com.ionista.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart();

    CartResponse addItem(AddToCartRequest request);

    CartResponse updateItem(Long itemId, UpdateCartItemRequest request);

    CartResponse removeItem(Long itemId);

    CartResponse clearCart();
}
