package com.example.dawanow.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MedicineRequestItemRequest(
        @NotNull Long productId,
        @NotNull @Positive Long quantity
) {
}
