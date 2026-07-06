package com.ionista.mapper;

import com.ionista.common.PriceUtils;
import com.ionista.dto.response.CartItemResponse;
import com.ionista.dto.response.CartResponse;
import com.ionista.entity.Cart;
import com.ionista.entity.CartItem;
import com.ionista.entity.ProductImage;
import com.ionista.entity.ProductVariant;
import com.ionista.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CartMapper {

    private final PricingService pricingService;

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .subtotal(subtotal)
                .build();
    }

    public CartItemResponse toItemResponse(CartItem item) {
        ProductVariant variant = item.getProductVariant();
        BigDecimal baseUnitPrice = PriceUtils.effectiveVariantPrice(variant);
        BigDecimal unitPrice = pricingService.effectiveUnitPrice(variant.getProduct(), baseUnitPrice);
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        String imageUrl = variant.getProduct().getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .or(() -> variant.getProduct().getImages().stream().min(Comparator.comparingInt(ProductImage::getSortOrder)))
                .map(ProductImage::getUrl)
                .orElse(null);

        return CartItemResponse.builder()
                .id(item.getId())
                .variantId(variant.getId())
                .productId(variant.getProduct().getId())
                .productName(variant.getProduct().getName())
                .imageUrl(imageUrl)
                .size(variant.getSize())
                .color(variant.getColor())
                .stockAvailable(variant.getStockQuantity())
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .build();
    }
}
