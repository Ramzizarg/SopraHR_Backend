package com.example.reservationservice.repository;

import com.example.reservationservice.entity.Booking;
import com.example.reservationservice.entity.Desk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByDeskAndBookingDate(Desk desk, LocalDate bookingDate);
    List<Booking> findByDesk(Desk desk);
}