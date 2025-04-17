package com.example.reservationservice.repository;

import com.example.reservationservice.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findByPlanId(String planId);
}