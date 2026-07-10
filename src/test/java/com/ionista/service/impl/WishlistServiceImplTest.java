package com.ionista.service.impl;

import com.ionista.entity.Product;
import com.ionista.entity.ProductImage;
import com.ionista.entity.User;
import com.ionista.entity.WishlistItem;
import com.ionista.exception.ConflictException;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.ProductRepository;
import com.ionista.repository.UserRepository;
import com.ionista.repository.WishlistItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().email("jane@example.com").build();
        user.setId(1L);

        var principal = org.springframework.security.core.userdetails.User
                .withUsername("jane@example.com").password("x").authorities("ROLE_USER").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Product buildProduct(Long id) {
        Product product = Product.builder().name("Shirt").slug("shirt")
                .basePrice(BigDecimal.valueOf(500)).active(true).build();
        product.setId(id);
        ProductImage image = ProductImage.builder().product(product).url("http://img").isPrimary(true).build();
        product.getImages().add(image);
        return product;
    }

    @Test
    void list_returnsMappedWishlistItems() {
        Product product = buildProduct(2L);
        WishlistItem item = WishlistItem.builder().user(user).product(product).build();
        when(wishlistItemRepository.findByUserId(1L)).thenReturn(List.of(item));

        List<?> result = wishlistService.list();

        assertThat(result).hasSize(1);
    }

    @Test
    void add_savesWishlistItem_whenNotAlreadyPresent() {
        Product product = buildProduct(2L);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(wishlistItemRepository.existsByUserIdAndProductId(1L, 2L)).thenReturn(false);

        wishlistService.add(2L);

        verify(wishlistItemRepository).save(argThat(item -> item.getUser() == user && item.getProduct() == product));
    }

    @Test
    void add_throws_whenProductNotFound() {
        when(productRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.add(2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void add_throws_whenAlreadyInWishlist() {
        Product product = buildProduct(2L);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(wishlistItemRepository.existsByUserIdAndProductId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> wishlistService.add(2L))
                .isInstanceOf(ConflictException.class);

        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void remove_deletesByUserAndProduct() {
        wishlistService.remove(2L);

        verify(wishlistItemRepository).deleteByUserIdAndProductId(1L, 2L);
    }
}
