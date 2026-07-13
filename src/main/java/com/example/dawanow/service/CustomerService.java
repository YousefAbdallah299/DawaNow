package com.example.dawanow.service;

import com.example.dawanow.dtos.request.UpdateCustomerProfileRequest;
import com.example.dawanow.dtos.response.CustomerResponse;
import com.example.dawanow.dtos.response.PaginatedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    public CustomerResponse getCurrentCustomer() {
        return null;
    }

    public CustomerResponse updateCurrentCustomer(UpdateCustomerProfileRequest request) {
        return null;
    }

    public PaginatedResponse<CustomerResponse> getAllCustomers(Pageable pageable) {
        return PaginatedResponse.empty(pageable);
    }

    public CustomerResponse getCustomerById(Long id) {
        return null;
    }

    public void deleteCustomer(Long id) {
    }
}
