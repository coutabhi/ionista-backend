package com.ionista.service.impl;

import com.ionista.dto.request.OfferRequest;
import com.ionista.dto.response.OfferResponse;
import com.ionista.entity.Category;
import com.ionista.entity.Offer;
import com.ionista.entity.Product;
import com.ionista.enums.DiscountType;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.CategoryRepository;
import com.ionista.repository.OfferRepository;
import com.ionista.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferServiceImplTest {

    @Mock
    private OfferRepository offerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private OfferServiceImpl offerService;

    private Offer buildOffer(Long id) {
        Offer offer = Offer.builder().name("Sale").discountType(DiscountType.PERCENTAGE)
                .value(BigDecimal.TEN)
                .startsAt(LocalDateTime.now().minusDays(1)).endsAt(LocalDateTime.now().plusDays(1))
                .active(true).build();
        offer.setId(id);
        return offer;
    }

    @Test
    void listActive_returnsMappedOffers() {
        Offer offer = buildOffer(1L);
        when(offerRepository.findActiveOffersNow(any())).thenReturn(List.of(offer));

        List<OfferResponse> result = offerService.listActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Sale");
    }

    @Test
    void create_resolvesProductsAndCategories() {
        Product product = Product.builder().name("Shirt").build();
        product.setId(10L);
        Category category = Category.builder().name("Men").build();
        category.setId(20L);

        OfferRequest request = OfferRequest.builder()
                .name("Flash Sale").discountType(DiscountType.FLAT).value(BigDecimal.valueOf(100))
                .startsAt(LocalDateTime.now()).endsAt(LocalDateTime.now().plusDays(1))
                .productIds(List.of(10L)).categoryIds(List.of(20L))
                .build();

        when(productRepository.findAllById(List.of(10L))).thenReturn(List.of(product));
        when(categoryRepository.findAllById(List.of(20L))).thenReturn(List.of(category));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        OfferResponse result = offerService.create(request);

        assertThat(result.getProductIds()).containsExactly(10L);
        assertThat(result.getCategoryIds()).containsExactly(20L);
    }

    @Test
    void create_handlesNoProductsOrCategories() {
        OfferRequest request = OfferRequest.builder()
                .name("Flash Sale").discountType(DiscountType.FLAT).value(BigDecimal.valueOf(100))
                .startsAt(LocalDateTime.now()).endsAt(LocalDateTime.now().plusDays(1))
                .build();

        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        OfferResponse result = offerService.create(request);

        assertThat(result.getProductIds()).isEmpty();
        assertThat(result.getCategoryIds()).isEmpty();
        verify(productRepository, never()).findAllById(any());
    }

    @Test
    void update_throws_whenOfferNotFound() {
        when(offerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.update(1L, OfferRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_appliesPartialChanges() {
        Offer offer = buildOffer(1L);
        OfferRequest request = OfferRequest.builder().active(false).build();
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        offerService.update(1L, request);

        assertThat(offer.isActive()).isFalse();
        assertThat(offer.getName()).isEqualTo("Sale");
    }

    @Test
    void delete_deactivatesOffer() {
        Offer offer = buildOffer(1L);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        offerService.delete(1L);

        assertThat(offer.isActive()).isFalse();
        verify(offerRepository).save(offer);
    }
}
