package com.example.dawanow.dtos.request;

import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateProductRequest(
        String name,
        @PositiveOrZero BigDecimal price,
        String imagePath,
        Long categoryId,
        String description,
        String company
) {
}
