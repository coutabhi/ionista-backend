package com.ionista.service.impl;

import com.ionista.dto.request.ProductRequest;
import com.ionista.dto.request.ProductVariantRequest;
import com.ionista.dto.response.ImageUploadResult;
import com.ionista.dto.response.ProductDetailResponse;
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
import com.ionista.service.CloudinaryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private ProductServiceImpl productService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "pw", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private void authenticateAsUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "pw", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private Category buildCategory(Long id) {
        Category category = Category.builder().name("Men").slug("men").active(true).build();
        category.setId(id);
        return category;
    }

    private Product buildProduct(Long id, boolean active) {
        Product product = Product.builder()
                .name("Shirt").description("desc").brand("Nike")
                .category(buildCategory(1L)).gender(Gender.MEN)
                .basePrice(BigDecimal.valueOf(1000))
                .sku("SKU1").slug("shirt").active(active)
                .build();
        product.setId(id);
        return product;
    }

    @Test
    void getById_returnsProduct_whenActive() {
        Product product = buildProduct(1L, true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toDetail(product)).thenReturn(ProductDetailResponse.builder().id(1L).build());

        ProductDetailResponse result = productService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getById_throws_whenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_throws_whenInactiveAndCallerNotAdmin() {
        Product product = buildProduct(1L, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        authenticateAsUser();

        assertThatThrownBy(() -> productService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_returnsInactiveProduct_whenCallerIsAdmin() {
        Product product = buildProduct(1L, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toDetail(product)).thenReturn(ProductDetailResponse.builder().id(1L).build());
        authenticateAsAdmin();

        ProductDetailResponse result = productService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getBySlug_throws_whenNotFound() {
        when(productRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getBySlug("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_savesProduct_whenSlugAndSkuAreUnique() {
        Category category = buildCategory(1L);
        ProductRequest request = ProductRequest.builder()
                .name("Shirt").description("desc").categoryId(1L).gender(Gender.MEN)
                .basePrice(BigDecimal.valueOf(1000)).sku("SKU1").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug("shirt")).thenReturn(false);
        when(productRepository.existsBySku("SKU1")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toDetail(any(Product.class))).thenReturn(ProductDetailResponse.builder().build());

        productService.create(request);

        verify(productRepository).save(argThat(p -> p.getSlug().equals("shirt") && p.getSku().equals("SKU1")));
    }

    @Test
    void create_throws_whenCategoryNotFound() {
        ProductRequest request = ProductRequest.builder().name("Shirt").categoryId(99L).build();
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_throws_whenSlugAlreadyExists() {
        Category category = buildCategory(1L);
        ProductRequest request = ProductRequest.builder()
                .name("Shirt").categoryId(1L).sku("SKU1").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug("shirt")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_throws_whenSkuAlreadyExists() {
        Category category = buildCategory(1L);
        ProductRequest request = ProductRequest.builder()
                .name("Shirt").categoryId(1L).sku("SKU1").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug("shirt")).thenReturn(false);
        when(productRepository.existsBySku("SKU1")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void delete_deactivatesProduct_insteadOfHardDelete() {
        Product product = buildProduct(1L, true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.delete(1L);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void addVariant_throws_whenSkuAlreadyExists() {
        Product product = buildProduct(1L, true);
        ProductVariantRequest request = ProductVariantRequest.builder()
                .size("M").color("Red").stockQuantity(10).sku("VSKU1").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productVariantRepository.existsBySku("VSKU1")).thenReturn(true);

        assertThatThrownBy(() -> productService.addVariant(1L, request))
                .isInstanceOf(ConflictException.class);

        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void addVariant_savesVariant_andReturnsRefreshedProduct() {
        Product product = buildProduct(1L, true);
        ProductVariantRequest request = ProductVariantRequest.builder()
                .size("M").color("Red").stockQuantity(10).sku("VSKU1").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productVariantRepository.existsBySku("VSKU1")).thenReturn(false);
        when(productMapper.toDetail(product)).thenReturn(ProductDetailResponse.builder().id(1L).build());

        ProductDetailResponse result = productService.addVariant(1L, request);

        assertThat(result.getId()).isEqualTo(1L);
        verify(productVariantRepository).save(argThat(v -> v.getSku().equals("VSKU1") && v.getStockQuantity() == 10));
    }

    @Test
    void updateVariant_throws_whenVariantNotFound() {
        ProductVariantRequest request = ProductVariantRequest.builder().build();
        when(productVariantRepository.findByIdAndProductId(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateVariant(1L, 2L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteVariant_removesVariant_whenFound() {
        Product product = buildProduct(1L, true);
        ProductVariant variant = ProductVariant.builder().product(product).size("M").color("Red").sku("VSKU1").build();
        variant.setId(2L);

        when(productVariantRepository.findByIdAndProductId(2L, 1L)).thenReturn(Optional.of(variant));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toDetail(product)).thenReturn(ProductDetailResponse.builder().build());

        productService.deleteVariant(1L, 2L);

        verify(productVariantRepository).delete(variant);
    }

    @Test
    void addImage_uploadsToCloudinary_andMarksPrimary() {
        Product product = buildProduct(1L, true);
        MultipartFile file = mock(MultipartFile.class);
        ImageUploadResult uploadResult = ImageUploadResult.builder().url("http://img").publicId("pub-1").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cloudinaryService.upload(eq(file), anyString())).thenReturn(uploadResult);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toDetail(product)).thenReturn(ProductDetailResponse.builder().build());

        productService.addImage(1L, file, true);

        assertThat(product.getImages()).hasSize(1);
        ProductImage added = product.getImages().get(0);
        assertThat(added.getUrl()).isEqualTo("http://img");
        assertThat(added.isPrimary()).isTrue();
    }

    @Test
    void deleteImage_deletesFromCloudinaryAndRepository() {
        Product product = buildProduct(1L, true);
        ProductImage image = ProductImage.builder().product(product).url("http://img").publicId("pub-1").build();
        image.setId(5L);

        when(productImageRepository.findByIdAndProductId(5L, 1L)).thenReturn(Optional.of(image));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toDetail(product)).thenReturn(ProductDetailResponse.builder().build());

        productService.deleteImage(1L, 5L);

        verify(cloudinaryService).delete("pub-1");
        verify(productImageRepository).delete(image);
    }

    @Test
    void deleteImage_throws_whenImageNotFound() {
        when(productImageRepository.findByIdAndProductId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteImage(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cloudinaryService, never()).delete(anyString());
    }
}
