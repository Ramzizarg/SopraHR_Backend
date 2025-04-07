package com.example.userservice.entity;

public enum Role {
    EMPLOYEE, MANAGER, ADMIN;

    public static Role fromString(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        String normalizedRole = role.trim().toUpperCase();
        System.out.println("Role input: " + role + ", normalized: " + normalizedRole);
        try {
            return Role.valueOf(normalizedRole);
        } catch (IllegalArgumentException e) {
            System.out.println("Failed to parse role: " + role);
            throw new IllegalArgumentException("Invalid role: " + role, e);
        }
    }

    @Override
    public String toString() {
        return name();
    }
}