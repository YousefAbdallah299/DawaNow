package com.example.dawanow.dtos.response;

import com.example.dawanow.entity.RequestStatus;
import java.util.List;

public record MedicineRequestResponse(
        Long id,
        Long customerId,
        Long pharmacyId,
        RequestStatus status,
        List<MedicineRequestItemResponse> items
) {
}
