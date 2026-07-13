package com.example.dawanow.service;

import com.example.dawanow.dtos.request.CreateOrderRequest;
import com.example.dawanow.dtos.response.OrderResponse;
import com.example.dawanow.dtos.response.PaginatedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    public OrderResponse createOrder(CreateOrderRequest request) {
        return null;
    }

    public PaginatedResponse<OrderResponse> getCurrentCustomerOrders(Pageable pageable) {
        return PaginatedResponse.empty(pageable);
    }

    public PaginatedResponse<OrderResponse> getPharmacyOrders(Long pharmacyId, Pageable pageable) {
        return PaginatedResponse.empty(pageable);
    }

    public PaginatedResponse<OrderResponse> getAllOrders(Pageable pageable) {
        return PaginatedResponse.empty(pageable);
    }

    public OrderResponse getOrderById(Long id) {
        return null;
    }
}
