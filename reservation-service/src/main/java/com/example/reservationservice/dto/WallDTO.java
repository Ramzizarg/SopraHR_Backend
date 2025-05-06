package com.example.reservationservice.dto;

public class WallDTO {
    private Long id;
    private String wallId;
    private double left;
    private double top;
    private double width;
    private double height;
    
    public WallDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWallId() {
        return wallId;
    }

    public void setWallId(String wallId) {
        this.wallId = wallId;
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


}
