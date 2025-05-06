package com.example.reservationservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "walls")
public class Wall {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String wallId;
    @Column(name = "`left`")
    private double left;
    
    @Column(name = "`top`")
    private double top;
    
    @Column(name = "`width`")
    private double width;
    
    @Column(name = "`height`")
    private double height;
    
    @ManyToOne
    @JoinColumn(name = "plan_id")
    private Plan plan;

    public Wall() {
    }
    
    public Wall(String wallId, double left, double top, double width, double height) {
        this.wallId = wallId;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
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



    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }
}
