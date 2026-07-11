package com.ionista.service.impl;

import com.ionista.common.SecurityUtils;
import com.ionista.common.SlugUtils;
import com.ionista.dto.request.ProductRequest;
import com.ionista.dto.request.ProductVariantRequest;
import com.ionista.dto.response.ImageUploadResult;
import com.ionista.dto.response.PageResponse;
import com.ionista.dto.response.ProductDetailResponse;
import com.ionista.dto.response.ProductSummaryResponse;
import com.ionista.entity.Category;
import com.ionista.entity.Product;
import com.ionista.entity.ProductImage;
import com.ionista.entity.ProductVariant;
import com.ionista.enums.Gender;
import com.ionista.exception.ConflictException;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.mapper.ProductMapper;
import com.ionista.repository.CategoryRepository;
import com.ionista.repository.ProductImageRepository;
import com.ionista.repository.ProductRepository;
import com.ionista.repository.ProductVariantRepository;
import com.ionista.repository.spec.ProductSpecifications;
import com.ionista.service.CloudinaryService;
import com.ionista.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductMapper productMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    public PageResponse<ProductSummaryResponse> list(Long categoryId, Gender gender, String brand,
                                                       BigDecimal minPrice, BigDecimal maxPrice,
                                                       String size, String color, String keyword,
                                                       Boolean featured, Pageable pageable) {
        Specification<Product> spec = Specification.where(ProductSpecifications.isActive())
                .and(ProductSpecifications.hasCategory(categoryId))
                .and(ProductSpecifications.hasGender(gender))
                .and(ProductSpecifications.hasBrand(brand))
                .and(ProductSpecifications.priceBetween(minPrice, maxPrice))
                .and(ProductSpecifications.hasSize(size))
                .and(ProductSpecifications.hasColor(color))
                .and(ProductSpecifications.nameContains(keyword))
                .and(ProductSpecifications.isFeatured(featured));

        Page<Product> page = productRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(productMapper::toSummary));
    }

    @Override
    public PageResponse<ProductSummaryResponse> listByCategory(Long categoryId, Pageable pageable) {
        Specification<Product> spec = Specification.where(ProductSpecifications.isActive())
                .and(ProductSpecifications.hasCategory(categoryId));

        Page<Product> page = productRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(productMapper::toSummary));
    }

    @Override
    public ProductDetailResponse getById(Long id) {
        return productMapper.toDetail(findVisibleProduct(id));
    }

    @Override
    public ProductDetailResponse getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        assertVisible(product);
        return productMapper.toDetail(product);
    }

    @Override
    public ProductDetailResponse create(ProductRequest request) {
        Category category = findCategory(request.getCategoryId());

        String slug = resolveSlug(request.getSlug(), request.getName());
        if (productRepository.existsBySlug(slug)) {
            throw new ConflictException("A product with slug '" + slug + "' already exists");
        }
        if (productRepository.existsBySku(request.getSku())) {
            throw new ConflictException("A product with SKU '" + request.getSku() + "' already exists");
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .brand(request.getBrand())
                .category(category)
                .gender(request.getGender())
                .basePrice(request.getBasePrice())
                .discountPrice(request.getDiscountPrice())
                .sku(request.getSku())
                .slug(slug)
                .active(request.getActive() == null || request.getActive())
                .featured(request.getFeatured() != null && request.getFeatured())
                .build();

        Product saved = productRepository.save(product);
        return productMapper.toDetail(saved);
    }

    @Override
    public ProductDetailResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (request.getCategoryId() != null) {
            product.setCategory(findCategory(request.getCategoryId()));
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
        }
        if (request.getGender() != null) {
            product.setGender(request.getGender());
        }
        if (request.getBasePrice() != null) {
            product.setBasePrice(request.getBasePrice());
        }
        product.setDiscountPrice(request.getDiscountPrice());

        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            if (productRepository.existsBySku(request.getSku())) {
                throw new ConflictException("A product with SKU '" + request.getSku() + "' already exists");
            }
            product.setSku(request.getSku());
        }

        String newSlug = resolveSlug(request.getSlug(), product.getName());
        if (!newSlug.equals(product.getSlug())) {
            if (productRepository.existsBySlug(newSlug)) {
                throw new ConflictException("A product with slug '" + newSlug + "' already exists");
            }
            product.setSlug(newSlug);
        }

        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        if (request.getFeatured() != null) {
            product.setFeatured(request.getFeatured());
        }

        return productMapper.toDetail(productRepository.save(product));
    }

    @Override
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    public ProductDetailResponse addVariant(Long productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (productVariantRepository.existsBySku(request.getSku())) {
            throw new ConflictException("A variant with SKU '" + request.getSku() + "' already exists");
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .size(request.getSize())
                .color(request.getColor())
                .stockQuantity(request.getStockQuantity())
                .priceOverride(request.getPriceOverride())
                .sku(request.getSku())
                .build();

        productVariantRepository.save(variant);
        return getById(productId);
    }

    @Override
    public ProductDetailResponse updateVariant(Long productId, Long variantId, ProductVariantRequest request) {
        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        if (request.getSize() != null) {
            variant.setSize(request.getSize());
        }
        if (request.getColor() != null) {
            variant.setColor(request.getColor());
        }
        if (request.getStockQuantity() != null) {
            variant.setStockQuantity(request.getStockQuantity());
        }
        variant.setPriceOverride(request.getPriceOverride());
        if (request.getSku() != null && !request.getSku().equals(variant.getSku())) {
            if (productVariantRepository.existsBySku(request.getSku())) {
                throw new ConflictException("A variant with SKU '" + request.getSku() + "' already exists");
            }
            variant.setSku(request.getSku());
        }

        productVariantRepository.save(variant);
        return getById(productId);
    }

    @Override
    public ProductDetailResponse deleteVariant(Long productId, Long variantId) {
        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        productVariantRepository.delete(variant);
        return getById(productId);
    }

    @Override
    @Transactional
    public ProductDetailResponse addImage(Long productId, MultipartFile file, boolean primary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        ImageUploadResult uploadResult = cloudinaryService.upload(file, "ionista/products/" + productId);

        if (primary) {
            product.getImages().forEach(image -> image.setPrimary(false));
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .url(uploadResult.getUrl())
                .publicId(uploadResult.getPublicId())
                .isPrimary(primary || product.getImages().isEmpty())
                .sortOrder(product.getImages().size())
                .build();

        product.getImages().add(image);
        productRepository.save(product);
        return getById(productId);
    }

    @Override
    @Transactional
    public ProductDetailResponse deleteImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        cloudinaryService.delete(image.getPublicId());
        productImageRepository.delete(image);
        return getById(productId);
    }

    private Product findVisibleProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        assertVisible(product);
        return product;
    }

    private void assertVisible(Product product) {
        if (!product.isActive() && !SecurityUtils.isAdmin()) {
            throw new ResourceNotFoundException("Product not found with id: " + product.getId());
        }
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    private String resolveSlug(String providedSlug, String name) {
        return SlugUtils.slugify((providedSlug == null || providedSlug.isBlank()) ? name : providedSlug);
    }
}
