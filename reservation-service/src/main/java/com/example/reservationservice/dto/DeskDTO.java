package com.example.reservationservice.dto;

import lombok.Data;

@Data
public class DeskDTO {
    private Long id;
    private double left;
    private double top;
    private double rotation;
    private String status;
    private String employeeName;
    private String duration;
    private String bookingDate;
}