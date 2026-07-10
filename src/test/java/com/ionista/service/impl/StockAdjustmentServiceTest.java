package com.ionista.service.impl;

import com.ionista.entity.ProductVariant;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAdjustmentServiceTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private StockAdjustmentService stockAdjustmentService;

    private ProductVariant buildVariant(int stock) {
        ProductVariant variant = ProductVariant.builder().size("M").color("Red").sku("SKU1").stockQuantity(stock).build();
        variant.setId(1L);
        return variant;
    }

    @Test
    void decrementStock_reducesQuantity() {
        ProductVariant variant = buildVariant(10);
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));

        stockAdjustmentService.decrementStock(1L, 3);

        assertThat(variant.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void decrementStock_neverGoesBelowZero() {
        ProductVariant variant = buildVariant(2);
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));

        stockAdjustmentService.decrementStock(1L, 5);

        assertThat(variant.getStockQuantity()).isEqualTo(0);
    }

    @Test
    void decrementStock_throws_whenVariantNotFound() {
        when(productVariantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockAdjustmentService.decrementStock(1L, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void restoreStock_increasesQuantity() {
        ProductVariant variant = buildVariant(5);
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));

        stockAdjustmentService.restoreStock(1L, 4);

        assertThat(variant.getStockQuantity()).isEqualTo(9);
    }

    @Test
    void restoreStock_throws_whenVariantNotFound() {
        when(productVariantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockAdjustmentService.restoreStock(1L, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
