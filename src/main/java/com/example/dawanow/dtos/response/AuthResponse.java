package com.example.dawanow.dtos.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}
