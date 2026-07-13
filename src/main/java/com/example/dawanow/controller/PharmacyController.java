package com.example.dawanow.controller;

import com.example.dawanow.dtos.request.CreatePharmacyRequest;
import com.example.dawanow.dtos.request.UpdatePharmacyRequest;
import com.example.dawanow.dtos.response.ApiResponse;
import com.example.dawanow.dtos.response.PaginatedResponse;
import com.example.dawanow.dtos.response.PharmacyResponse;
import com.example.dawanow.service.PharmacyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pharmacies")
@RequiredArgsConstructor
public class PharmacyController {

    private final PharmacyService pharmacyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaginatedResponse<PharmacyResponse>>> getAllPharmacies(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success("Pharmacies fetched", pharmacyService.getAllPharmacies(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<PharmacyResponse>> getPharmacyById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Pharmacy fetched", pharmacyService.getPharmacyById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PharmacyResponse>> createPharmacy(@Valid @RequestBody CreatePharmacyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Pharmacy created", pharmacyService.createPharmacy(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PharmacyResponse>> updatePharmacy(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePharmacyRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Pharmacy updated", pharmacyService.updatePharmacy(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePharmacy(@PathVariable Long id) {
        pharmacyService.deletePharmacy(id);
        return ResponseEntity.ok(ApiResponse.success("Pharmacy deleted"));
    }
}
