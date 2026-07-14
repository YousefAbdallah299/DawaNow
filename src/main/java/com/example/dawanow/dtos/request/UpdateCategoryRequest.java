package com.example.dawanow.dtos.request;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(max = 255) String name
) {
}
