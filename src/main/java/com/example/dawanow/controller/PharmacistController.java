package com.example.dawanow.controller;

import com.example.dawanow.dtos.request.AssignPharmacistToPharmacyRequest;
import com.example.dawanow.dtos.response.ApiResponse;
import com.example.dawanow.dtos.response.PaginatedResponse;
import com.example.dawanow.dtos.response.PharmacistResponse;
import com.example.dawanow.service.PharmacistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pharmacists")
@RequiredArgsConstructor
public class PharmacistController {

    private final PharmacistService pharmacistService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<ApiResponse<PharmacistResponse>> getCurrentPharmacist() {
        return ResponseEntity.ok(ApiResponse.success("Current pharmacist fetched", pharmacistService.getCurrentPharmacist()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaginatedResponse<PharmacistResponse>>> getAllPharmacists(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success("Pharmacists fetched", pharmacistService.getAllPharmacists(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PharmacistResponse>> getPharmacistById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Pharmacist fetched", pharmacistService.getPharmacistById(id)));
    }

    //id here is for pharmacist not pharmacy
    @PatchMapping("/{id}/pharmacy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PharmacistResponse>> assignPharmacistToPharmacy(
            @PathVariable Long id,
            @Valid @RequestBody AssignPharmacistToPharmacyRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Pharmacist assigned to pharmacy", pharmacistService.assignToPharmacy(id, request)));
    }
}
