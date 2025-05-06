package com.example.reservationservice.repository;

import com.example.reservationservice.model.Desk;
import com.example.reservationservice.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByDesk(Desk desk);
    List<Reservation> findByBookingDate(String bookingDate);
    List<Reservation> findByUserId(Long userId);
    
    @Query("SELECT r FROM Reservation r WHERE r.desk.id = :deskId AND r.bookingDate = :bookingDate")
    List<Reservation> findReservationsByDeskAndDate(Long deskId, String bookingDate);
    
    @Query("SELECT r FROM Reservation r WHERE r.desk.plan.id = :planId AND r.bookingDate = :bookingDate")
    List<Reservation> findReservationsByPlanAndDate(Long planId, String bookingDate);
    
    @Query("SELECT r FROM Reservation r WHERE r.userId = :userId AND r.bookingDate >= :startDate AND r.bookingDate <= :endDate")
    List<Reservation> findUserReservationsInDateRange(Long userId, String startDate, String endDate);
    
    @Query("SELECT r FROM Reservation r WHERE r.userId = :userId AND r.bookingDate = :bookingDate")
    List<Reservation> findByUserIdAndBookingDate(Long userId, String bookingDate);
}
