package com.example.notificationservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.notificationservice.dto.CreateNotificationDTO;
import com.example.notificationservice.dto.NotificationDTO;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationDTO> createNotification(@RequestBody CreateNotificationDTO createNotificationDTO) {
        log.info("Creating notification for user: {}", createNotificationDTO.getUserId());
        NotificationDTO notification = notificationService.createNotification(createNotificationDTO);
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationDTO>> getUserNotifications(@PathVariable Long userId) {
        log.info("Fetching notifications for user: {}", userId);
        List<NotificationDTO> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationDTO>> getUserUnreadNotifications(@PathVariable Long userId) {
        log.info("Fetching unread notifications for user: {}", userId);
        List<NotificationDTO> notifications = notificationService.getUserUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Long> getUnreadNotificationCount(@PathVariable Long userId) {
        log.info("Fetching unread notification count for user: {}", userId);
        long count = notificationService.getUnreadNotificationCount(userId);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable Long notificationId) {
        log.info("Marking notification as read: {}", notificationId);
        NotificationDTO notification = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/user/{userId}/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId) {
        log.info("Marking all notifications as read for user: {}", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        log.info("Deleting notification: {}", notificationId);
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<NotificationDTO>> getNotificationsByType(
            @PathVariable Long userId, 
            @PathVariable Notification.NotificationType type) {
        log.info("Fetching notifications of type {} for user: {}", type, userId);
        List<NotificationDTO> notifications = notificationService.getNotificationsByType(userId, type);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/entity/{relatedEntityId}/type/{relatedEntityType}")
    public ResponseEntity<List<NotificationDTO>> getNotificationsByRelatedEntity(
            @PathVariable Long relatedEntityId, 
            @PathVariable String relatedEntityType) {
        log.info("Fetching notifications for entity: {} of type: {}", relatedEntityId, relatedEntityType);
        List<NotificationDTO> notifications = notificationService.getNotificationsByRelatedEntity(relatedEntityId, relatedEntityType);
        return ResponseEntity.ok(notifications);
    }
} 