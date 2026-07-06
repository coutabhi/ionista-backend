package com.ionista.service;

import com.ionista.entity.Product;

import java.math.BigDecimal;

public interface PricingService {

    BigDecimal effectiveUnitPrice(Product product, BigDecimal baseUnitPrice);

    BigDecimal bestOfferDiscountPerUnit(Product product, BigDecimal baseUnitPrice);
}
