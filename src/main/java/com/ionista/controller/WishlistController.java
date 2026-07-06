package com.ionista.controller;

import com.ionista.dto.response.WishlistItemResponse;
import com.ionista.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> list() {
        return ResponseEntity.ok(wishlistService.list());
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Void> add(@PathVariable Long productId) {
        wishlistService.add(productId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(@PathVariable Long productId) {
        wishlistService.remove(productId);
        return ResponseEntity.noContent().build();
    }
}
