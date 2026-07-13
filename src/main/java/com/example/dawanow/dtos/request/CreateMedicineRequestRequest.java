package com.example.dawanow.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateMedicineRequestRequest(
        @NotNull Long pharmacyId,
        @Valid @NotEmpty List<MedicineRequestItemRequest> items
) {
}
