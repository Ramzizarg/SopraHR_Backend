package com.example.reservationservice.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlanDTO {
    private String id;
    private double left;
    private double top;
    private double width;
    private double height;
    private List<DeskDTO> desks = new ArrayList<>();
    private List<WallDTO> walls = new ArrayList<>();
}