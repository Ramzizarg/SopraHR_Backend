package com.example.notificationservice.dto;

import com.example.notificationservice.model.Notification;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationDTO {
    private Long userId;
    private String title;
    private String message;
    private Notification.NotificationType type;
    private Long relatedEntityId;
    private String relatedEntityType;
} 