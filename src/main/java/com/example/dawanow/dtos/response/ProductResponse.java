package com.example.dawanow.dtos.response;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        String imagePath,
        Long categoryId,
        String description,
        String company
) {
}
