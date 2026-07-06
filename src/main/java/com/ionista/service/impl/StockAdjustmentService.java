package com.ionista.service.impl;

import com.ionista.entity.ProductVariant;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StockAdjustmentService {

    private final ProductVariantRepository productVariantRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decrementStock(Long variantId, int quantity) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
        variant.setStockQuantity(Math.max(variant.getStockQuantity() - quantity, 0));
        productVariantRepository.save(variant);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreStock(Long variantId, int quantity) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
        variant.setStockQuantity(variant.getStockQuantity() + quantity);
        productVariantRepository.save(variant);
    }
}
