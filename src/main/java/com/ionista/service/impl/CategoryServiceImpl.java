package com.ionista.service.impl;

import com.ionista.common.SlugUtils;
import com.ionista.dto.request.CategoryRequest;
import com.ionista.dto.response.CategoryResponse;
import com.ionista.entity.Category;
import com.ionista.exception.ConflictException;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.mapper.CategoryMapper;
import com.ionista.repository.CategoryRepository;
import com.ionista.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> listTopLevel() {
        return categoryRepository.findByParentIsNull().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> listByParent(Long parentId) {
        return categoryRepository.findByParentId(parentId).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getById(Long id) {
        return categoryMapper.toResponse(findCategory(id));
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        String slug = resolveSlug(request.getSlug(), request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            throw new ConflictException("A category with slug '" + slug + "' already exists");
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = findCategory(request.getParentId());
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .parent(parent)
                .active(request.getActive() == null || request.getActive())
                .build();

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findCategory(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            category.setName(request.getName());
        }

        String newSlug = resolveSlug(request.getSlug(), category.getName());
        if (!newSlug.equals(category.getSlug()) && categoryRepository.existsBySlug(newSlug)) {
            throw new ConflictException("A category with slug '" + newSlug + "' already exists");
        }
        category.setSlug(newSlug);

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new ConflictException("A category cannot be its own parent");
            }
            category.setParent(findCategory(request.getParentId()));
        }

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public void delete(Long id) {
        Category category = findCategory(id);
        categoryRepository.delete(category);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private String resolveSlug(String providedSlug, String name) {
        return SlugUtils.slugify((providedSlug == null || providedSlug.isBlank()) ? name : providedSlug);
    }
}
