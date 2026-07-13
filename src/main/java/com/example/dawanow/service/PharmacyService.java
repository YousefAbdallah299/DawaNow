package com.example.dawanow.service;

import com.example.dawanow.dtos.request.CreatePharmacyRequest;
import com.example.dawanow.dtos.request.UpdatePharmacyRequest;
import com.example.dawanow.dtos.response.PaginatedResponse;
import com.example.dawanow.dtos.response.PharmacyResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PharmacyService {

    public PaginatedResponse<PharmacyResponse> getAllPharmacies(Pageable pageable) {
        return PaginatedResponse.empty(pageable);
    }

    public PharmacyResponse getPharmacyById(Long id) {
        return null;
    }

    public PharmacyResponse createPharmacy(CreatePharmacyRequest request) {
        return null;
    }

    public PharmacyResponse updatePharmacy(Long id, UpdatePharmacyRequest request) {
        return null;
    }

    public void deletePharmacy(Long id) {
    }
}
