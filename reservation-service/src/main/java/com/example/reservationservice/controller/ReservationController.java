package com.example.reservationservice.controller;

import com.example.reservationservice.dto.BookingRequestDTO;
import com.example.reservationservice.dto.PlanDTO;
import com.example.reservationservice.service.ReservationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservation")
@Slf4j
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/plans")
    public ResponseEntity<?> createPlan(@Valid @RequestBody PlanDTO planDTO,
                                        @AuthenticationPrincipal String email) {
        try {
            PlanDTO createdPlan = reservationService.createPlan(planDTO, email);
            log.info("Plan created successfully: {}", createdPlan.getId());
            return ResponseEntity.ok(createdPlan);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create plan: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating plan: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error occurred"));
        }
    }

    @GetMapping("/plans")
    public ResponseEntity<List<PlanDTO>> getAllPlans() {
        try {
            List<PlanDTO> plans = reservationService.getAllPlans();
            log.info("Fetched {} plans", plans.size());
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            log.error("Failed to fetch plans: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/plans/{planId}")
    public ResponseEntity<?> updatePlan(@PathVariable String planId,
                                        @Valid @RequestBody PlanDTO planDTO,
                                        @AuthenticationPrincipal String email) {
        try {
            if (!planId.equals(planDTO.getId())) {
                throw new IllegalArgumentException("Plan ID mismatch");
            }
            PlanDTO updatedPlan = reservationService.updatePlan(planDTO);
            log.info("Plan updated successfully: {}", planId);
            return ResponseEntity.ok(updatedPlan);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update plan {}: {}", planId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating plan {}: {}", planId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error occurred"));
        }
    }

    @DeleteMapping("/plans/{planId}")
    public ResponseEntity<?> deletePlan(@PathVariable String planId) {
        try {
            reservationService.deletePlan(planId);
            log.info("Plan deleted successfully: {}", planId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete plan {}: {}", planId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error deleting plan {}: {}", planId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error occurred"));
        }
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> bookDesk(@Valid @RequestBody BookingRequestDTO request,
                                      @AuthenticationPrincipal String email) {
        try {
            reservationService.bookDesk(request, email);
            log.info("Desk {} booked successfully for user {}", request.getDeskId(), email);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to book desk {}: {}", request.getDeskId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error booking desk {}: {}", request.getDeskId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error occurred"));
        }
    }

    @DeleteMapping("/bookings/{deskId}/{bookingDate}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long deskId,
                                           @PathVariable String bookingDate) {
        try {
            reservationService.cancelBooking(deskId, bookingDate);
            log.info("Booking cancelled for desk {} on {}", deskId, bookingDate);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to cancel booking for desk {} on {}: {}",
                    deskId, bookingDate, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error cancelling booking for desk {} on {}: {}",
                    deskId, bookingDate, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error occurred"));
        }
    }
}