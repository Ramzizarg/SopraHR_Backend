package com.example.teletravailservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class TeletravailRequestDTO {
    private Long userId;
    private String travailType;
    private LocalDate teletravailDate;
    private String travailMaison;
    private String selectedPays;
    private String selectedGouvernorat;
    private String reason;

    public void setTeletravailDate(String teletravailDate) {
        this.teletravailDate = LocalDate.parse(teletravailDate);
    }
}