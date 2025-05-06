package com.example.planningservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanningGenerationRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long userId; // Optional, if null generate for all users
}
