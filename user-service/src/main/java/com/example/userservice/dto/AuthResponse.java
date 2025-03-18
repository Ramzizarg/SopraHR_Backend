package com.example.userservice.dto;

public record AuthResponse(
        String firstName,
        String lastName,
        String role,
        String accessToken,
        String refreshToken,
        String errorMessage
) {
    public AuthResponse(String firstName, String lastName, String role,
                        String accessToken, String refreshToken) {
        this(firstName, lastName, role, accessToken, refreshToken, null);
    }
}
