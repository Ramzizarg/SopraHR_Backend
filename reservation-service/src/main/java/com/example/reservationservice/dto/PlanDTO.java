package com.example.reservationservice.dto;

import java.util.ArrayList;
import java.util.List;

public class PlanDTO {
    private Long id;
    private String planId;
    private double left;
    private double top;
    private double width;
    private double height;
    private List<DeskDTO> desks = new ArrayList<>();
    private List<WallDTO> walls = new ArrayList<>();

    public PlanDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public double getLeft() {
        return left;
    }

    public void setLeft(double left) {
        this.left = left;
    }

    public double getTop() {
        return top;
    }

    public void setTop(double top) {
        this.top = top;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public List<DeskDTO> getDesks() {
        return desks;
    }

    public void setDesks(List<DeskDTO> desks) {
        this.desks = desks;
    }

    public List<WallDTO> getWalls() {
        return walls;
    }

    public void setWalls(List<WallDTO> walls) {
        this.walls = walls;
    }
}
