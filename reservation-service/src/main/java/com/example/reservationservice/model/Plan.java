package com.example.reservationservice.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plans")
public class Plan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String planId;
    @Column(name = "`left`")
    private double left;
    
    @Column(name = "`top`")
    private double top;
    
    @Column(name = "`width`")
    private double width;
    
    @Column(name = "`height`")
    private double height;
    
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Desk> desks = new ArrayList<>();
    
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wall> walls = new ArrayList<>();

    public Plan() {
    }
    
    public Plan(String planId, double left, double top, double width, double height) {
        this.planId = planId;
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

    public List<Desk> getDesks() {
        return desks;
    }

    public void setDesks(List<Desk> desks) {
        this.desks = desks;
    }

    public List<Wall> getWalls() {
        return walls;
    }

    public void setWalls(List<Wall> walls) {
        this.walls = walls;
    }
    
    public void addDesk(Desk desk) {
        desks.add(desk);
        desk.setPlan(this);
    }
    
    public void removeDesk(Desk desk) {
        desks.remove(desk);
        desk.setPlan(null);
    }
    
    public void addWall(Wall wall) {
        walls.add(wall);
        wall.setPlan(this);
    }
    
    public void removeWall(Wall wall) {
        walls.remove(wall);
        wall.setPlan(null);
    }
}
