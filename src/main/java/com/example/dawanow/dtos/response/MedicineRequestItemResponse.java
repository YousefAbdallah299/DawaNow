package com.example.dawanow.dtos.response;

import java.math.BigDecimal;

public record MedicineRequestItemResponse(
        Long id,
        Long productId,
        Long quantity,
        BigDecimal unitPrice
) {
}
