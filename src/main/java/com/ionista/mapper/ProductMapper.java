package com.ionista.mapper;

import com.ionista.common.PriceUtils;
import com.ionista.dto.response.ProductDetailResponse;
import com.ionista.dto.response.ProductImageResponse;
import com.ionista.dto.response.ProductSummaryResponse;
import com.ionista.dto.response.ProductVariantResponse;
import com.ionista.entity.Product;
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
public class ProductMapper {

    private final PricingService pricingService;

    public ProductSummaryResponse toSummary(Product product) {
        String primaryImageUrl = product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .or(() -> product.getImages().stream().min(Comparator.comparingInt(ProductImage::getSortOrder)))
                .map(ProductImage::getUrl)
                .orElse(null);

        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .brand(product.getBrand())
                .gender(product.getGender())
                .basePrice(product.getBasePrice())
                .discountPrice(product.getDiscountPrice())
                .effectivePrice(withOfferPrice(product))
                .primaryImageUrl(primaryImageUrl)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .active(product.isActive())
                .featured(product.isFeatured())
                .build();
    }

    public ProductDetailResponse toDetail(Product product) {
        List<ProductVariantResponse> variants = product.getVariants().stream()
                .map(this::toVariantResponse)
                .toList();

        List<ProductImageResponse> images = product.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getSortOrder))
                .map(this::toImageResponse)
                .toList();

        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .gender(product.getGender())
                .basePrice(product.getBasePrice())
                .discountPrice(product.getDiscountPrice())
                .effectivePrice(withOfferPrice(product))
                .sku(product.getSku())
                .slug(product.getSlug())
                .active(product.isActive())
                .featured(product.isFeatured())
                .variants(variants)
                .images(images)
                .build();
    }

    public ProductVariantResponse toVariantResponse(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .size(variant.getSize())
                .color(variant.getColor())
                .stockQuantity(variant.getStockQuantity())
                .priceOverride(variant.getPriceOverride())
                .sku(variant.getSku())
                .build();
    }

    public ProductImageResponse toImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .isPrimary(image.isPrimary())
                .sortOrder(image.getSortOrder())
                .build();
    }

    private BigDecimal withOfferPrice(Product product) {
        BigDecimal basePrice = PriceUtils.effectiveProductPrice(product);
        return pricingService.effectiveUnitPrice(product, basePrice);
    }

}
