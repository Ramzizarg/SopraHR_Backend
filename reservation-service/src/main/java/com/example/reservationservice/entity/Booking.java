package com.example.reservationservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "booking")
@Data
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "booking_date")
    private LocalDate bookingDate;

    @Column(name = "duration")
    private String duration;

    @ManyToOne
    @JoinColumn(name = "desk_id")
    private Desk desk;
}