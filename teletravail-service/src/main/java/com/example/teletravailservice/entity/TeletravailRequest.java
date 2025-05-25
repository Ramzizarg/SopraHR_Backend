package com.example.teletravailservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class TeletravailRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    
    // Employee name (firstName + lastName)
    private String employeeName;
    
    // Team name (e.g., "DEV", "QA", etc.)
    private String team;
    
    private Long teamLeaderId;

    private String travailType;

    private String teletravailDate;

    private String travailMaison;

    private String selectedPays;

    private String selectedGouvernorat;

    private String reason;
    
    @Enumerated(EnumType.STRING)
    private TeletravailStatus status = TeletravailStatus.PENDING;
    
    private String rejectionReason;
    
    private LocalDateTime statusUpdatedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum TeletravailStatus {
        PENDING,    // Awaiting team leader approval
        APPROVED,   // Approved by team leader
        REJECTED    // Rejected by team leader
    }
}