package com.ionista.service.impl;

import com.ionista.dto.request.CategoryRequest;
import com.ionista.dto.response.CategoryResponse;
import com.ionista.entity.Category;
import com.ionista.exception.ConflictException;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.mapper.CategoryMapper;
import com.ionista.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category buildCategory(Long id, String name, String slug) {
        Category category = Category.builder().name(name).slug(slug).active(true).build();
        category.setId(id);
        return category;
    }

    @Test
    void listTopLevel_returnsMappedCategories() {
        Category category = buildCategory(1L, "Men", "men");
        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(CategoryResponse.builder().id(1L).name("Men").slug("men").build());

        List<CategoryResponse> result = categoryService.listTopLevel();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Men");
    }

    @Test
    void listByParent_returnsMappedCategories() {
        Category category = buildCategory(2L, "Shirts", "shirts");
        when(categoryRepository.findByParentId(1L)).thenReturn(List.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(CategoryResponse.builder().id(2L).name("Shirts").slug("shirts").build());

        List<CategoryResponse> result = categoryService.listByParent(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSlug()).isEqualTo("shirts");
    }

    @Test
    void getById_returnsCategory_whenExists() {
        Category category = buildCategory(1L, "Men", "men");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(CategoryResponse.builder().id(1L).name("Men").build());

        CategoryResponse result = categoryService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getById_throws_whenNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_savesCategory_withSlugifiedName_whenSlugNotProvided() {
        CategoryRequest request = CategoryRequest.builder().name("Men's Wear").build();
        when(categoryRepository.existsBySlug("mens-wear")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryMapper.toResponse(any(Category.class))).thenReturn(CategoryResponse.builder().name("Men's Wear").slug("mens-wear").build());

        CategoryResponse result = categoryService.create(request);

        assertThat(result.getSlug()).isEqualTo("mens-wear");
        verify(categoryRepository).save(argThat(c -> c.getSlug().equals("mens-wear") && c.isActive()));
    }

    @Test
    void create_throws_whenSlugAlreadyExists() {
        CategoryRequest request = CategoryRequest.builder().name("Men").slug("men").build();
        when(categoryRepository.existsBySlug("men")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_throws_whenParentNotFound() {
        CategoryRequest request = CategoryRequest.builder().name("Shirts").parentId(5L).build();
        when(categoryRepository.existsBySlug("shirts")).thenReturn(false);
        when(categoryRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_linksParent_whenParentExists() {
        Category parent = buildCategory(1L, "Men", "men");
        CategoryRequest request = CategoryRequest.builder().name("Shirts").parentId(1L).build();
        when(categoryRepository.existsBySlug("shirts")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryMapper.toResponse(any(Category.class))).thenReturn(CategoryResponse.builder().build());

        categoryService.create(request);

        verify(categoryRepository).save(argThat(c -> c.getParent() == parent));
    }

    @Test
    void update_throws_whenCategoryIsOwnParent() {
        Category category = buildCategory(1L, "Men", "men");
        CategoryRequest request = CategoryRequest.builder().parentId(1L).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.update(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("own parent");
    }

    @Test
    void update_throws_whenNewSlugAlreadyTaken() {
        Category category = buildCategory(1L, "Men", "men");
        CategoryRequest request = CategoryRequest.builder().slug("women").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsBySlug("women")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_appliesPartialChanges() {
        Category category = buildCategory(1L, "Men", "men");
        CategoryRequest request = CategoryRequest.builder().active(false).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryMapper.toResponse(any(Category.class))).thenReturn(CategoryResponse.builder().build());

        categoryService.update(1L, request);

        assertThat(category.isActive()).isFalse();
        assertThat(category.getName()).isEqualTo("Men");
    }

    @Test
    void delete_removesCategory_whenExists() {
        Category category = buildCategory(1L, "Men", "men");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_throws_whenNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
