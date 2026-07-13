package com.example.dawanow.dtos.request;

import com.example.dawanow.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String password,
        @NotNull UserRole role,
        String homeAddress,
        LocalDate dob,
        Long pharmacyId
) {
}
