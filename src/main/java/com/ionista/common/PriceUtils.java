package com.ionista.common;

import com.ionista.entity.Product;
import com.ionista.entity.ProductVariant;

import java.math.BigDecimal;

public final class PriceUtils {

    private PriceUtils() {
    }

    public static BigDecimal effectiveProductPrice(Product product) {
        return product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getBasePrice();
    }

    public static BigDecimal effectiveVariantPrice(ProductVariant variant) {
        if (variant.getPriceOverride() != null) {
            return variant.getPriceOverride();
        }
        return effectiveProductPrice(variant.getProduct());
    }
}
