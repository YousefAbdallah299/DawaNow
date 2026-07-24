package com.example.dawanow.dtos.response;

import java.math.BigDecimal;

public record MedicineRequestResultItemResponse(
        Long productId,
        String productname,
        String imageUrl,
        BigDecimal unitPrice,
        Boolean alternative,
        Boolean available
) {

}
