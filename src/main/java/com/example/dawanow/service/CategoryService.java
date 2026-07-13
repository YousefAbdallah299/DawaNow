package com.example.dawanow.service;

import com.example.dawanow.dtos.request.CreateCategoryRequest;
import com.example.dawanow.dtos.request.UpdateCategoryRequest;
import com.example.dawanow.dtos.response.CategoryResponse;
import com.example.dawanow.dtos.response.PaginatedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    public PaginatedResponse<CategoryResponse> getAllCategories(Pageable pageable) {
        return PaginatedResponse.empty(pageable);
    }

    public CategoryResponse getCategoryById(Long id) {
        return null;
    }

    public CategoryResponse createCategory(CreateCategoryRequest request) {
        return null;
    }

    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        return null;
    }

    public void deleteCategory(Long id) {
    }
}
