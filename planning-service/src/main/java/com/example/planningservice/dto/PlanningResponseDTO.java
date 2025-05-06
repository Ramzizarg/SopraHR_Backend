package com.example.planningservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanningResponseDTO {
    private Long id;
    private Long userId;
    private String userName;
    private LocalDate planningDate;
    private String planningStatus;
    private String location;
    private String workType;
    private String reasons;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
