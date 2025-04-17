package com.example.reservationservice.repository;

import com.example.reservationservice.entity.Wall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WallRepository extends JpaRepository<Wall, Long> {
    Optional<Wall> findByWallId(String wallId);
}