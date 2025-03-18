package com.example.userservice.entity;

public enum Team {
    DEV,
    DEVOPS,
    BI,
    RH;

    // Optional: Add a method to convert from string (case-insensitive)
    public static Team fromString(String teamStr) {
        if (teamStr == null) {
            throw new IllegalArgumentException("Team cannot be null");
        }
        try {
            return Team.valueOf(teamStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid team: " + teamStr + ". Must be DEV, DEVOPS, BI, or RH");
        }
    }
}