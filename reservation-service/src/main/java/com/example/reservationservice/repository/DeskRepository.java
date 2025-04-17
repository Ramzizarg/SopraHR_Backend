package com.example.reservationservice.repository;

import com.example.reservationservice.entity.Desk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeskRepository extends JpaRepository<Desk, Long> {
    Optional<Desk> findByDeskId(Long deskId);
}