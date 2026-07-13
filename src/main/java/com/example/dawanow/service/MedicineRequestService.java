package com.example.dawanow.service;

import com.example.dawanow.dtos.request.CreateMedicineRequestRequest;
import com.example.dawanow.dtos.request.UpdateMedicineRequestStatusRequest;
import com.example.dawanow.dtos.response.MedicineRequestResponse;
import com.example.dawanow.dtos.response.PaginatedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MedicineRequestService {

    public MedicineRequestResponse createRequest(CreateMedicineRequestRequest request) {
        return null;
    }

    public PaginatedResponse<MedicineRequestResponse> getCurrentCustomerRequests(Pageable pageable) {
        return PaginatedResponse.empty(pageable);
    }

    public PaginatedResponse<MedicineRequestResponse> getAllRequests(Pageable pageable) {
        return PaginatedResponse.empty(pageable);
    }

    public PaginatedResponse<MedicineRequestResponse> getPharmacyRequests(Long pharmacyId, Pageable pageable) {
        return PaginatedResponse.empty(pageable);
    }

    public MedicineRequestResponse getRequestById(Long id) {
        return null;
    }

    public MedicineRequestResponse updateRequestStatus(Long id, UpdateMedicineRequestStatusRequest request) {
        return null;
    }
}
