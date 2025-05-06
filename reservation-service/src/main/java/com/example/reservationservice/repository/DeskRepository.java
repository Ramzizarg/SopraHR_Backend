package com.example.reservationservice.repository;

import com.example.reservationservice.model.Desk;
import com.example.reservationservice.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeskRepository extends JpaRepository<Desk, Long> {
    List<Desk> findByPlan(Plan plan);
    
    @Query("SELECT d FROM Desk d WHERE d.id = :deskId AND NOT EXISTS (SELECT r FROM Reservation r WHERE r.desk.id = d.id AND r.bookingDate = :bookingDate)")
    Desk findAvailableDeskForBooking(Long deskId, String bookingDate);
}
