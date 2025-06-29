package com.example.reservationservice.controller;

import com.example.reservationservice.dto.ReservationDTO;
import com.example.reservationservice.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

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

    @GetMapping("/daterange")
    public ResponseEntity<List<ReservationDTO>> getReservationsInDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(reservationService.getReservationsInDateRange(startDate, endDate, token));
    }

    @GetMapping("/analytics/dashboard")
    public ResponseEntity<Map<String, Object>> getAnalytics(HttpServletRequest request) {
        String token = extractToken(request);
        Map<String, Object> analytics = new HashMap<>();
        
        try {
            // Get current date and determine if it's weekend
            LocalDate today = LocalDate.now();
            DayOfWeek dayOfWeek = today.getDayOfWeek();
            
            // If it's weekend (Saturday = 6, Sunday = 7), show next week's data
            LocalDate targetWeekStart;
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                // It's weekend, so show next week
                int daysUntilNextMonday = dayOfWeek == DayOfWeek.SUNDAY ? 1 : 2; // Sunday = 1 day, Saturday = 2 days
                targetWeekStart = today.plusDays(daysUntilNextMonday);
            } else {
                // It's a weekday, show current week
                targetWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            }
            
            LocalDate targetWeekEnd = targetWeekStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
            
            // Get all reservations for the target week
            List<ReservationDTO> weekReservations = reservationService.getReservationsInDateRange(
                targetWeekStart.toString(), targetWeekEnd.toString(), token);
            
            // Calculate total reservations
            analytics.put("totalReservations", weekReservations.size());
            
            // Calculate today's reservations (or next Monday if it's weekend)
            LocalDate targetToday = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY ? 
                targetWeekStart : today;
            List<ReservationDTO> todayReservations = reservationService.getReservationsByDate(targetToday.toString(), token);
            analytics.put("todayReservations", todayReservations.size());
            
            // Calculate occupancy rate (assuming 50 desks total)
            int totalDesks = 50;
            double occupancyRate = todayReservations.size() > 0 ? 
                ((double) todayReservations.size() / totalDesks) * 100 : 0.0;
            analytics.put("occupancyRate", Math.round(occupancyRate));
            
            // Calculate weekly occupancy by day for the target week
            List<Map<String, Object>> weeklyOccupancy = new ArrayList<>();
            String[] daysOfWeek = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi"};
            
            for (int i = 0; i < 5; i++) {
                LocalDate dayDate = targetWeekStart.plusDays(i);
                List<ReservationDTO> dayReservations = reservationService.getReservationsByDate(dayDate.toString(), token);
                
                double dayOccupancy = dayReservations.size() > 0 ? 
                    ((double) dayReservations.size() / totalDesks) * 100 : 0.0;
                
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("day", daysOfWeek[i]);
                dayData.put("percentage", Math.round(dayOccupancy));
                weeklyOccupancy.add(dayData);
            }
            
            analytics.put("weeklyOccupancy", weeklyOccupancy);
            
            return ResponseEntity.ok(analytics);
            
        } catch (Exception e) {
            // Return default data if there's an error
            analytics.put("totalReservations", 0);
            analytics.put("todayReservations", 0);
            analytics.put("occupancyRate", 0.0);
            
            // Default weekly occupancy data
            List<Map<String, Object>> defaultWeeklyOccupancy = new ArrayList<>();
            String[] daysOfWeek = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi"};
            for (String day : daysOfWeek) {
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("day", day);
                dayData.put("percentage", 0);
                defaultWeeklyOccupancy.add(dayData);
            }
            analytics.put("weeklyOccupancy", defaultWeeklyOccupancy);
            
            return ResponseEntity.ok(analytics);
        }
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
