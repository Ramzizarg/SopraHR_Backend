package com.example.teletravailservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class NotificationClient {
    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
    
    @Value("${notification.service.url}")
    private String notificationServiceUrl;
    
    @Value("${user.service.url}")
    private String userServiceUrl;
    
    private final RestTemplate restTemplate;

    public NotificationClient(RestTemplate restTemplate, @Value("${notification.service.url}") String notificationServiceUrl, @Value("${user.service.url}") String userServiceUrl) {
        super();
        this.restTemplate = restTemplate;
        this.notificationServiceUrl = notificationServiceUrl;
        this.userServiceUrl = userServiceUrl;
    }

    public void createNotification(Long userId, String title, String message, String type, Long relatedEntityId, String relatedEntityType) {
        try {
            log.info("Creating notification for user: {} with title: {}", userId, title);
            
            Map<String, Object> notificationData = Map.of(
                "userId", userId,
                "title", title,
                "message", message,
                "type", type,
                "relatedEntityId", relatedEntityId,
                "relatedEntityType", relatedEntityType
            );
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(notificationData, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                notificationServiceUrl + "/api/notifications",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Notification created successfully for user: {}", userId);
            } else {
                log.warn("Failed to create notification for user: {}, status: {}", userId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error creating notification for user {}: {}", userId, e.getMessage());
        }
    }

    public void notifyTeamLeadersAndManagers(Long employeeId, String employeeName, String team, Long requestId, String date) {
        try {
            log.info("Notifying team leaders and managers for employee: {} in team: {}", employeeName, team);
            
            // Get team leaders and managers for the team
            List<Long> teamLeadersAndManagers = getTeamLeadersAndManagers(team);
            
            for (Long leaderId : teamLeadersAndManagers) {
                String title = "Nouvelle demande de télétravail";
                String message = String.format("L'employé %s a soumis une demande de télétravail pour le %s.", employeeName, date);
                
                createNotification(leaderId, title, message, "TELEWORK_REQUEST_CREATED", requestId, "TELEWORK_REQUEST");
            }
        } catch (Exception e) {
            log.error("Error notifying team leaders and managers: {}", e.getMessage());
        }
    }

    public void notifyEmployee(Long employeeId, String employeeName, String status, String date, String reason, Long requestId) {
        try {
            log.info("Notifying employee: {} about request status: {}", employeeName, status);
            
            String title;
            String message;
            String type;
            
            if ("APPROVED".equals(status)) {
                title = "Demande de télétravail approuvée";
                message = String.format("Votre demande de télétravail pour le %s a été approuvée.", date);
                type = "TELEWORK_REQUEST_APPROVED";
            } else {
                title = "Demande de télétravail refusée";
                message = String.format("Votre demande de télétravail pour le %s a été refusée.", date);
                if (reason != null && !reason.trim().isEmpty()) {
                    message += " Raison: " + reason;
                }
                type = "TELEWORK_REQUEST_REJECTED";
            }
            
            createNotification(employeeId, title, message, type, requestId, "TELEWORK_REQUEST");
        } catch (Exception e) {
            log.error("Error notifying employee: {}", e.getMessage());
        }
    }

    private List<Long> getTeamLeadersAndManagers(String team) {
        try {
            log.info("Getting team leaders and managers for team: {}", team);
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<Long>> response = restTemplate.exchange(
                userServiceUrl + "/public/team/" + team + "/leaders-managers",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Found {} team leaders and managers for team: {}", response.getBody().size(), team);
                return response.getBody();
            }
            
            log.warn("Could not get team leaders and managers for team: {}", team);
            return List.of();
        } catch (Exception e) {
            log.error("Error getting team leaders and managers for team {}: {}", team, e.getMessage());
            return List.of();
        }
    }
} 