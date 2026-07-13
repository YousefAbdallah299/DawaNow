package com.example.dawanow.dtos.request;

import com.example.dawanow.entity.RequestStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateMedicineRequestStatusRequest(
        @NotNull RequestStatus status
) {
}
