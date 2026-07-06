package com.ionista.controller;

import com.ionista.dto.request.AddToCartRequest;
import com.ionista.dto.request.UpdateCartItemRequest;
import com.ionista.dto.response.CartResponse;
import com.ionista.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItem(request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(@PathVariable Long itemId, @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(itemId));
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart() {
        return ResponseEntity.ok(cartService.clearCart());
    }
}
