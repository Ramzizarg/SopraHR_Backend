package com.example.reservationservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "wall")
@Data
public class Wall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wall_id")
    private String wallId;

    @Column(name = "x_position", nullable = false)
    private double xPosition;

    @Column(name = "top", nullable = false)
    private double top;

    @Column(name = "width", nullable = false)
    private double width;

    @Column(name = "height", nullable = false)
    private double height;

    @Column(name = "rotation", nullable = false)
    private double rotation;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private Plan plan;
}