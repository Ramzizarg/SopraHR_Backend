package com.example.notificationservice.service;

import com.example.notificationservice.dto.CreateNotificationDTO;
import com.example.notificationservice.dto.NotificationDTO;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public NotificationDTO createNotification(CreateNotificationDTO createNotificationDTO) {
        log.info("Creating notification for user: {}", createNotificationDTO.getUserId());
        
        Notification notification = new Notification();
        notification.setUserId(createNotificationDTO.getUserId());
        notification.setTitle(createNotificationDTO.getTitle());
        notification.setMessage(createNotificationDTO.getMessage());
        notification.setType(createNotificationDTO.getType());
        notification.setStatus(Notification.NotificationStatus.UNREAD);
        notification.setRelatedEntityId(createNotificationDTO.getRelatedEntityId());
        notification.setRelatedEntityType(createNotificationDTO.getRelatedEntityType());
        
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification created with ID: {}", savedNotification.getId());
        
        return NotificationDTO.fromEntity(savedNotification);
    }

    public List<NotificationDTO> getUserNotifications(Long userId) {
        log.info("Fetching notifications for user: {}", userId);
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getUserUnreadNotifications(Long userId) {
        log.info("Fetching unread notifications for user: {}", userId);
        List<Notification> notifications = notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, Notification.NotificationStatus.UNREAD);
        return notifications.stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public long getUnreadNotificationCount(Long userId) {
        return notificationRepository.countUnreadNotificationsByUserId(userId);
    }

    public NotificationDTO markAsRead(Long notificationId) {
        log.info("Marking notification as read: {}", notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        
        notification.setStatus(Notification.NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        
        Notification savedNotification = notificationRepository.save(notification);
        return NotificationDTO.fromEntity(savedNotification);
    }

    public void markAllAsRead(Long userId) {
        log.info("Marking all notifications as read for user: {}", userId);
        
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, Notification.NotificationStatus.UNREAD);
        
        for (Notification notification : unreadNotifications) {
            notification.setStatus(Notification.NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());
        }
        
        notificationRepository.saveAll(unreadNotifications);
    }

    public void deleteNotification(Long notificationId) {
        log.info("Deleting notification: {}", notificationId);
        notificationRepository.deleteById(notificationId);
    }

    public List<NotificationDTO> getNotificationsByType(Long userId, Notification.NotificationType type) {
        log.info("Fetching notifications of type {} for user: {}", type, userId);
        List<Notification> notifications = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
        return notifications.stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getNotificationsByRelatedEntity(Long relatedEntityId, String relatedEntityType) {
        log.info("Fetching notifications for entity: {} of type: {}", relatedEntityId, relatedEntityType);
        List<Notification> notifications = notificationRepository.findByRelatedEntityIdAndRelatedEntityType(
                relatedEntityId, relatedEntityType);
        return notifications.stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }
} 