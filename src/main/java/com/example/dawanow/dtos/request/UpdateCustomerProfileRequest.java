package com.example.dawanow.dtos.request;

import java.time.LocalDate;

public record UpdateCustomerProfileRequest(
        String homeAddress,
        LocalDate dob
) {
}
