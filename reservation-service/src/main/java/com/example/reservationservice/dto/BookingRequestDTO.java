package com.example.reservationservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookingRequestDTO {
    private Long deskId;
    private String employeeName;
    private String duration;
    private List<String> bookingDates;
}