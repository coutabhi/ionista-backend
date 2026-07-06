package com.ionista.service.impl;

import com.ionista.common.PriceUtils;
import com.ionista.common.SecurityUtils;
import com.ionista.dto.response.WishlistItemResponse;
import com.ionista.entity.Product;
import com.ionista.entity.ProductImage;
import com.ionista.entity.User;
import com.ionista.entity.WishlistItem;
import com.ionista.exception.ConflictException;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.ProductRepository;
import com.ionista.repository.UserRepository;
import com.ionista.repository.WishlistItemRepository;
import com.ionista.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public List<WishlistItemResponse> list() {
        User user = currentUser();
        return wishlistItemRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void add(Long productId) {
        User user = currentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (wishlistItemRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new ConflictException("Product is already in your wishlist");
        }

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .build();
        wishlistItemRepository.save(item);
    }

    @Override
    public void remove(Long productId) {
        User user = currentUser();
        wishlistItemRepository.deleteByUserIdAndProductId(user.getId(), productId);
    }

    private User currentUser() {
        return userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        Product product = item.getProduct();
        String imageUrl = product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .or(() -> product.getImages().stream().min(Comparator.comparingInt(ProductImage::getSortOrder)))
                .map(ProductImage::getUrl)
                .orElse(null);

        return WishlistItemResponse.builder()
                .productId(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .imageUrl(imageUrl)
                .effectivePrice(PriceUtils.effectiveProductPrice(product))
                .active(product.isActive())
                .addedAt(item.getCreatedAt())
                .build();
    }
}
