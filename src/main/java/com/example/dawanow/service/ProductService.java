package com.example.dawanow.service;

import com.example.dawanow.dtos.request.CreateProductRequest;
import com.example.dawanow.dtos.request.UpdateProductRequest;
import com.example.dawanow.dtos.response.PaginatedResponse;
import com.example.dawanow.dtos.response.ProductResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    public PaginatedResponse<ProductResponse> getAllProducts(Pageable pageable) {
        return PaginatedResponse.empty(pageable);
    }

    public ProductResponse getProductById(Long id) {
        return null;
    }

    public ProductResponse createProduct(CreateProductRequest request) {
        return null;
    }

    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        return null;
    }

    public void deleteProduct(Long id) {
    }
}
