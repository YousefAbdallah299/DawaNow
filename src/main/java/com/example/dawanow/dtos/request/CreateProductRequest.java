package com.example.dawanow.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotBlank String imagePath,
        @NotNull Long categoryId,
        @NotBlank String description,
        @NotBlank String company
) {
}
