package com.example.teletravailservice.dto;

import com.example.teletravailservice.entity.TeletravailRequest;
import com.example.teletravailservice.entity.TeletravailRequest.TeletravailStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeletravailResponseDTO {
    private Long id;
    private Long userId;
    private String userName; // Will be populated from user service if available
    // Team name (e.g., "DEV", "QA", etc.)
    private String team;
    private Long teamLeaderId;
    private String teamLeaderName; // Will be populated from user service if available
    private String travailType;
    private String teletravailDate;
    private String travailMaison;
    private String selectedPays;
    private String selectedGouvernorat;
    private String reason;
    private TeletravailStatus status;
    private String rejectionReason;
    private LocalDateTime statusUpdatedAt;
    private LocalDateTime createdAt;
    private String message;
    private String errorMessage;

    public TeletravailResponseDTO(TeletravailRequest request) {
        this.id = request.getId();
        this.userId = request.getUserId();
        
        // Set userName from employeeName if available
        if (request.getEmployeeName() != null && !request.getEmployeeName().isEmpty() 
                && !"Unknown User".equals(request.getEmployeeName())) {
            this.userName = request.getEmployeeName();
        } else {
            // For the frontend, provide the user's email or ID if we don't have a name
            // This prevents the frontend from showing "Utilisateur inconnu"
            this.userName = "Utilisateur #" + request.getUserId();
        }
        
        this.team = request.getTeam();
        this.travailType = request.getTravailType();
        this.teletravailDate = request.getTeletravailDate();
        this.travailMaison = request.getTravailMaison();
        this.selectedPays = request.getSelectedPays();
        this.selectedGouvernorat = request.getSelectedGouvernorat();
        this.reason = request.getReason();
        this.status = request.getStatus();
        this.rejectionReason = request.getRejectionReason();
        this.statusUpdatedAt = request.getStatusUpdatedAt();
        this.createdAt = request.getCreatedAt();
    }

    public TeletravailResponseDTO(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public TeletravailResponseDTO(String message, String errorMessage) {
        this.message = message;
        this.errorMessage = errorMessage;
    }
}