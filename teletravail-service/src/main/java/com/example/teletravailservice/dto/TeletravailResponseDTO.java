package com.example.teletravailservice.dto;

import com.example.teletravailservice.entity.TeletravailRequest;
import lombok.Data;

@Data
public class TeletravailResponseDTO {
    private Long id;
    private String travailType;
    private String teletravailDate;
    private String travailMaison;
    private String selectedPays;
    private String selectedGouvernorat;
    private String reason;
    private String message;
    private String errorMessage;

    public TeletravailResponseDTO(TeletravailRequest request) {
        this.id = request.getId();
        this.travailType = request.getTravailType();
        this.teletravailDate = request.getTeletravailDate();
        this.travailMaison = request.getTravailMaison();
        this.selectedPays = request.getSelectedPays();
        this.selectedGouvernorat = request.getSelectedGouvernorat();
        this.reason = request.getReason();
    }

    public TeletravailResponseDTO(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public TeletravailResponseDTO(String message, String errorMessage) {
        this.message = message;
        this.errorMessage = errorMessage;
    }
}