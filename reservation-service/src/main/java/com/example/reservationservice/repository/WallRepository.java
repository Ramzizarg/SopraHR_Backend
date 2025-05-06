package com.example.reservationservice.repository;

import com.example.reservationservice.model.Plan;
import com.example.reservationservice.model.Wall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WallRepository extends JpaRepository<Wall, Long> {
    List<Wall> findByPlan(Plan plan);
    Optional<Wall> findByWallId(String wallId);
}
