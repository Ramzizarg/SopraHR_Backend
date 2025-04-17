package com.example.reservationservice.dto;

import lombok.Data;

@Data
public class WallDTO {
    private String id;
    private double left;
    private double top;
    private double width;
    private double height;
    private double rotation;
}