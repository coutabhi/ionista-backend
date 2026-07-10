package com.ionista.service.impl;

import com.ionista.entity.Category;
import com.ionista.entity.Offer;
import com.ionista.entity.Product;
import com.ionista.enums.DiscountType;
import com.ionista.repository.OfferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceImplTest {

    @Mock
    private OfferRepository offerRepository;

    @InjectMocks
    private PricingServiceImpl pricingService;

    private Product buildProduct() {
        Category category = Category.builder().name("Men").slug("men").build();
        category.setId(1L);
        Product product = Product.builder().name("Shirt").category(category).basePrice(BigDecimal.valueOf(1000)).build();
        product.setId(1L);
        return product;
    }

    private Offer percentageOffer(BigDecimal percent) {
        return Offer.builder().name("Sale").discountType(DiscountType.PERCENTAGE).value(percent)
                .startsAt(LocalDateTime.now().minusDays(1)).endsAt(LocalDateTime.now().plusDays(1)).build();
    }

    private Offer flatOffer(BigDecimal flat) {
        return Offer.builder().name("Flat").discountType(DiscountType.FLAT).value(flat)
                .startsAt(LocalDateTime.now().minusDays(1)).endsAt(LocalDateTime.now().plusDays(1)).build();
    }

    @Test
    void bestOfferDiscountPerUnit_returnsZero_whenNoActiveOffers() {
        Product product = buildProduct();
        when(offerRepository.findActiveOffersForProductOrCategory(any(), any(), any())).thenReturn(List.of());

        BigDecimal discount = pricingService.bestOfferDiscountPerUnit(product, BigDecimal.valueOf(1000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void bestOfferDiscountPerUnit_computesPercentageDiscount() {
        Product product = buildProduct();
        when(offerRepository.findActiveOffersForProductOrCategory(any(), any(), any()))
                .thenReturn(List.of(percentageOffer(BigDecimal.valueOf(10))));

        BigDecimal discount = pricingService.bestOfferDiscountPerUnit(product, BigDecimal.valueOf(1000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(100).setScale(2));
    }

    @Test
    void bestOfferDiscountPerUnit_pickBestAmongMultipleOffers() {
        Product product = buildProduct();
        when(offerRepository.findActiveOffersForProductOrCategory(any(), any(), any()))
                .thenReturn(List.of(percentageOffer(BigDecimal.valueOf(5)), flatOffer(BigDecimal.valueOf(200))));

        BigDecimal discount = pricingService.bestOfferDiscountPerUnit(product, BigDecimal.valueOf(1000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    void bestOfferDiscountPerUnit_capsAtBaseUnitPrice() {
        Product product = buildProduct();
        when(offerRepository.findActiveOffersForProductOrCategory(any(), any(), any()))
                .thenReturn(List.of(flatOffer(BigDecimal.valueOf(5000))));

        BigDecimal discount = pricingService.bestOfferDiscountPerUnit(product, BigDecimal.valueOf(1000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void effectiveUnitPrice_subtractsDiscountFromBasePrice() {
        Product product = buildProduct();
        when(offerRepository.findActiveOffersForProductOrCategory(any(), any(), any()))
                .thenReturn(List.of(flatOffer(BigDecimal.valueOf(300))));

        BigDecimal price = pricingService.effectiveUnitPrice(product, BigDecimal.valueOf(1000));

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(700));
    }

    @Test
    void effectiveUnitPrice_neverGoesNegative() {
        Product product = buildProduct();
        when(offerRepository.findActiveOffersForProductOrCategory(any(), any(), any()))
                .thenReturn(List.of(flatOffer(BigDecimal.valueOf(5000))));

        BigDecimal price = pricingService.effectiveUnitPrice(product, BigDecimal.valueOf(1000));

        assertThat(price).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
