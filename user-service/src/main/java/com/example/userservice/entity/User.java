package com.example.userservice.entity;

import lombok.Data;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

@Entity
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)  // Stores the enum as a string in the database
    private Team team;

    private String resetToken;
    private LocalDateTime tokenExpiry;
    
    private String profilePhotoUrl;
}