package com.example.notificationservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // Recipient user ID
    
    private String title;
    
    @Column(length = 1000)
    private String message;
    
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.UNREAD;
    
    private Long relatedEntityId; // ID of the related entity (e.g., teletravail request ID)
    
    private String relatedEntityType; // Type of related entity (e.g., "TELEWORK_REQUEST")
    
    private LocalDateTime createdAt;
    
    private LocalDateTime readAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum NotificationType {
        TELEWORK_REQUEST_CREATED,    // Employee created a telework request
        TELEWORK_REQUEST_APPROVED,   // Team leader/manager approved the request
        TELEWORK_REQUEST_REJECTED    // Team leader/manager rejected the request
    }
    
    public enum NotificationStatus {
        UNREAD,
        READ
    }
} 