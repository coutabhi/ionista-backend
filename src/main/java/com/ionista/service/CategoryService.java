package com.ionista.service;

import com.ionista.dto.request.CategoryRequest;
import com.ionista.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> listTopLevel();

    List<CategoryResponse> listByParent(Long parentId);

    CategoryResponse getById(Long id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);
}
