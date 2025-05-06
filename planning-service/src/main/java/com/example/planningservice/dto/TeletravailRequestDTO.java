package com.example.planningservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeletravailRequestDTO {
    private Long id;
    private Long userId;
    private String travailType;
    private String teletravailDate;
    private String travailMaison;
    private String selectedPays;
    private String selectedGouvernorat;
    private String reason;
    private LocalDateTime createdAt;
}
