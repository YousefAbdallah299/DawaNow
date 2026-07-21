package com.example.dawanow.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOfferItemRequest(@NotNull
                                        Long requestItemId,

                                     @Positive
                                        Long quantity,

                                     @Positive
                                     BigDecimal unitPrice,

                                     Long productId) {
}
