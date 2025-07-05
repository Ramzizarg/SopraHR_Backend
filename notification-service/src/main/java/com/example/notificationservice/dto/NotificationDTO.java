package com.example.notificationservice.dto;

import com.example.notificationservice.model.Notification;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private Notification.NotificationType type;
    private Notification.NotificationStatus status;
    private Long relatedEntityId;
    private String relatedEntityType;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    
    public static NotificationDTO fromEntity(Notification notification) {
        return new NotificationDTO(
            notification.getId(),
            notification.getUserId(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getType(),
            notification.getStatus(),
            notification.getRelatedEntityId(),
            notification.getRelatedEntityType(),
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }
    
    public Notification toEntity() {
        Notification notification = new Notification();
        notification.setId(this.id);
        notification.setUserId(this.userId);
        notification.setTitle(this.title);
        notification.setMessage(this.message);
        notification.setType(this.type);
        notification.setStatus(this.status);
        notification.setRelatedEntityId(this.relatedEntityId);
        notification.setRelatedEntityType(this.relatedEntityType);
        notification.setCreatedAt(this.createdAt);
        notification.setReadAt(this.readAt);
        return notification;
    }
} 