package com.example.reservationservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "desk")
@Data
public class Desk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "desk_id")
    private Long deskId;

    @Column(name = "x_position", nullable = false)
    private double xPosition;

    @Column(name = "top", nullable = false)
    private double top;

    @Column(name = "rotation", nullable = false)
    private double rotation;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private Plan plan;
}