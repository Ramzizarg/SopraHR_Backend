package com.example.userservice.dto;

public record UserResponse(
        Long id,
        String email,
        String role,
        String firstName,
        String lastName,
        String team,
        String errorMessage
) {
    public UserResponse(Long id, String email, String role, String firstName,
                        String lastName, String team) {
        this(id, email, role, firstName, lastName, team, null);
    }
}

