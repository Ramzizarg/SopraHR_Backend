package com.example.teletravailservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeletravailRequestDTO {
    @NotBlank(message = "Travail type is required")
    private String travailType;

    @NotBlank(message = "Teletravail date is required")
    private String teletravailDate;

    @NotBlank(message = "Travail maison is required")
    private String travailMaison;

    private String selectedPays;

    private String selectedGouvernorat;

    private String reason;
    
    // These fields are set by the service based on user information
    // Team name (e.g., "DEV", "QA", etc.)
    private String team;
    
    private Long teamLeaderId;
    
    // For status updates and rejections
    private String status;
    private String rejectionReason;
}