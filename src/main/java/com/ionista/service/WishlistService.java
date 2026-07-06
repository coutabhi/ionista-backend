package com.ionista.service;

import com.ionista.dto.response.WishlistItemResponse;

import java.util.List;

public interface WishlistService {

    List<WishlistItemResponse> list();

    void add(Long productId);

    void remove(Long productId);
}
