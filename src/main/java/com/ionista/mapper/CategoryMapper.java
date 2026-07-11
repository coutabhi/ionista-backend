package com.ionista.mapper;

import com.ionista.dto.response.CategoryResponse;
import com.ionista.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .active(category.isActive())
                .imageUrl(category.getImageUrl())
                .build();
    }
}
