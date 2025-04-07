package com.example.userservice.entity;

public enum Team {
    DEV, QA, OPS, RH;

    public static Team fromString(String team) {
        if (team == null || team.trim().isEmpty()) {
            throw new IllegalArgumentException("Team cannot be null or empty");
        }
        try {
            return Team.valueOf(team.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid team: " + team, e);
        }
    }

    @Override
    public String toString() {
        return name();
    }
}