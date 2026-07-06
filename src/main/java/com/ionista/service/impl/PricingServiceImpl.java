package com.ionista.service.impl;

import com.ionista.entity.Offer;
import com.ionista.entity.Product;
import com.ionista.enums.DiscountType;
import com.ionista.repository.OfferRepository;
import com.ionista.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private final OfferRepository offerRepository;

    @Override
    public BigDecimal effectiveUnitPrice(Product product, BigDecimal baseUnitPrice) {
        BigDecimal discount = bestOfferDiscountPerUnit(product, baseUnitPrice);
        BigDecimal effective = baseUnitPrice.subtract(discount);
        return effective.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : effective;
    }

    @Override
    public BigDecimal bestOfferDiscountPerUnit(Product product, BigDecimal baseUnitPrice) {
        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        List<Offer> offers = offerRepository.findActiveOffersForProductOrCategory(product.getId(), categoryId, LocalDateTime.now());

        BigDecimal best = BigDecimal.ZERO;
        for (Offer offer : offers) {
            BigDecimal discount = offer.getDiscountType() == DiscountType.PERCENTAGE
                    ? baseUnitPrice.multiply(offer.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : offer.getValue();
            if (discount.compareTo(best) > 0) {
                best = discount;
            }
        }
        return best.min(baseUnitPrice);
    }
}
