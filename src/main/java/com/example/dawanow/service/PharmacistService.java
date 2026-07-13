package com.example.dawanow.service;

import com.example.dawanow.dtos.request.AssignPharmacistToPharmacyRequest;
import com.example.dawanow.dtos.response.PaginatedResponse;
import com.example.dawanow.dtos.response.PharmacistResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PharmacistService {

    public PharmacistResponse getCurrentPharmacist() {
        return null;
    }

    public PaginatedResponse<PharmacistResponse> getAllPharmacists(Pageable pageable) {
        return PaginatedResponse.empty(pageable);
    }

    public PharmacistResponse getPharmacistById(Long id) {
        return null;
    }

    public PharmacistResponse assignToPharmacy(Long id, AssignPharmacistToPharmacyRequest request) {
        return null;
    }
}
