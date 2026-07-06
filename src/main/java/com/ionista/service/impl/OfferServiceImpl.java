package com.ionista.service.impl;

import com.ionista.dto.request.OfferRequest;
import com.ionista.dto.response.OfferResponse;
import com.ionista.entity.Category;
import com.ionista.entity.Offer;
import com.ionista.entity.Product;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.CategoryRepository;
import com.ionista.repository.OfferRepository;
import com.ionista.repository.ProductRepository;
import com.ionista.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OfferResponse> listAll() {
        return offerRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfferResponse> listActive() {
        return offerRepository.findActiveOffersNow(LocalDateTime.now()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public OfferResponse create(OfferRequest request) {
        Offer offer = Offer.builder()
                .name(request.getName())
                .discountType(request.getDiscountType())
                .value(request.getValue())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .active(request.getActive() == null || request.getActive())
                .products(resolveProducts(request.getProductIds()))
                .categories(resolveCategories(request.getCategoryIds()))
                .build();

        return toResponse(offerRepository.save(offer));
    }

    @Override
    public OfferResponse update(Long id, OfferRequest request) {
        Offer offer = findOffer(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            offer.setName(request.getName());
        }
        if (request.getDiscountType() != null) {
            offer.setDiscountType(request.getDiscountType());
        }
        if (request.getValue() != null) {
            offer.setValue(request.getValue());
        }
        if (request.getStartsAt() != null) {
            offer.setStartsAt(request.getStartsAt());
        }
        if (request.getEndsAt() != null) {
            offer.setEndsAt(request.getEndsAt());
        }
        if (request.getActive() != null) {
            offer.setActive(request.getActive());
        }
        if (request.getProductIds() != null) {
            offer.setProducts(resolveProducts(request.getProductIds()));
        }
        if (request.getCategoryIds() != null) {
            offer.setCategories(resolveCategories(request.getCategoryIds()));
        }

        return toResponse(offerRepository.save(offer));
    }

    @Override
    public void delete(Long id) {
        Offer offer = findOffer(id);
        offer.setActive(false);
        offerRepository.save(offer);
    }

    private Offer findOffer(Long id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));
    }

    private Set<Product> resolveProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(productRepository.findAllById(productIds));
    }

    private Set<Category> resolveCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(categoryRepository.findAllById(categoryIds));
    }

    private OfferResponse toResponse(Offer offer) {
        return OfferResponse.builder()
                .id(offer.getId())
                .name(offer.getName())
                .discountType(offer.getDiscountType())
                .value(offer.getValue())
                .startsAt(offer.getStartsAt())
                .endsAt(offer.getEndsAt())
                .active(offer.isActive())
                .productIds(offer.getProducts().stream().map(Product::getId).toList())
                .categoryIds(offer.getCategories().stream().map(Category::getId).toList())
                .build();
    }
}
