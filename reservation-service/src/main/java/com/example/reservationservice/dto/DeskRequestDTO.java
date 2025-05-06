package com.example.reservationservice.dto;


import jakarta.validation.constraints.NotNull;

public class DeskRequestDTO {
    
    @NotNull(message = "Left position is required")
    private Double left;
    
    @NotNull(message = "Top position is required")
    private Double top;
    
    @NotNull(message = "Rotation is required")
    private Double rotation;

    public DeskRequestDTO() {
    }

    public Double getLeft() {
        return left;
    }

    public void setLeft(Double left) {
        this.left = left;
    }

    public Double getTop() {
        return top;
    }

    public void setTop(Double top) {
        this.top = top;
    }

    public Double getRotation() {
        return rotation;
    }

    public void setRotation(Double rotation) {
        this.rotation = rotation;
    }
}
