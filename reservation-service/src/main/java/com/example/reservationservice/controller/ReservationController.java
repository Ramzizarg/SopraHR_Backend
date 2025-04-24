package com.example.reservationservice.controller;

import com.example.reservationservice.dto.ReservationDTO;
import com.example.reservationservice.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @Autowired
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<List<ReservationDTO>> getAllReservations(HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(reservationService.getAllReservations(token));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getReservationById(
            @PathVariable Long id,
            HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(reservationService.getReservationById(id, token));
    }
    
    @GetMapping("/date")
    public ResponseEntity<List<ReservationDTO>> getReservationsByDate(
            @RequestParam String date,
            HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(reservationService.getReservationsByDate(date, token));
    }
    
    @GetMapping("/user")
    public ResponseEntity<List<ReservationDTO>> getUserReservations(HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(reservationService.getUserReservations(token));
    }
    
    @GetMapping("/user/daterange")
    public ResponseEntity<List<ReservationDTO>> getUserReservationsInDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(reservationService.getUserReservationsInDateRange(startDate, endDate, token));
    }

    @PostMapping
    public ResponseEntity<ReservationDTO> createReservation(
            @RequestBody ReservationDTO reservationDTO,
            HttpServletRequest request) {
        String token = extractToken(request);
        return new ResponseEntity<>(reservationService.createReservation(reservationDTO, token), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationDTO> updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationDTO reservationDTO,
            HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(reservationService.updateReservation(id, reservationDTO, token));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(
            @PathVariable Long id,
            HttpServletRequest request) {
        String token = extractToken(request);
        reservationService.deleteReservation(id, token);
        return ResponseEntity.noContent().build();
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
