package com.example.userservice.entity;

public enum Role {
    ADMIN,
    MANAGER,
    EMPLOYEE;

    // Optional: Add a method to convert from string (case-insensitive)
    public static Role fromString(String roleStr) {
        if (roleStr == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        try {
            return Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleStr + ". Must be ADMIN, MANAGER, or EMPLOYEE");
        }
    }
}