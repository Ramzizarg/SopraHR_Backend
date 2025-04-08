package com.example.teletravailservice.entity;



import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "teletravail_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeletravailRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String travailType;

    @Column(nullable = false)
    private LocalDate teletravailDate;

    @Column(nullable = false)
    private String travailMaison;

    private String selectedPays;

    private String selectedGouvernorat;

    private String reason;
}