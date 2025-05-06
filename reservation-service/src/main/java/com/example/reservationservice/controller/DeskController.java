package com.example.reservationservice.controller;

import com.example.reservationservice.dto.DeskDTO;
import com.example.reservationservice.service.DeskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/v1/desks")
public class DeskController {

    private final DeskService deskService;

    @Autowired
    public DeskController(DeskService deskService) {
        this.deskService = deskService;
    }

    @GetMapping
    public ResponseEntity<List<DeskDTO>> getAllDesks() {
        return ResponseEntity.ok(deskService.getAllDesks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeskDTO> getDeskById(@PathVariable Long id) {
        return ResponseEntity.ok(deskService.getDeskById(id));
    }
    
    @GetMapping("/plan/{planId}")
    public ResponseEntity<List<DeskDTO>> getDesksByPlanId(@PathVariable Long planId) {
        return ResponseEntity.ok(deskService.getDesksByPlanId(planId));
    }
    
    @GetMapping("/{id}/availability")
    public ResponseEntity<DeskDTO> getDeskAvailabilityForDate(
            @PathVariable Long id,
            @RequestParam String date) {
        return ResponseEntity.ok(deskService.getDeskAvailabilityForDate(id, date));
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<DeskDTO>> getAvailableDesksByPlanAndDate(
            @RequestParam Long planId,
            @RequestParam String date) {
        return ResponseEntity.ok(deskService.getAvailableDesksByPlanAndDate(planId, date));
    }

    @PostMapping("/plan/{planId}")
    public ResponseEntity<DeskDTO> createDesk(
            @PathVariable Long planId,
            @RequestBody DeskDTO deskDTO,
            HttpServletRequest request) {
        String token = extractToken(request);
        return new ResponseEntity<>(deskService.createDesk(planId, deskDTO, token), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeskDTO> updateDesk(
            @PathVariable Long id,
            @RequestBody DeskDTO deskDTO,
            HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(deskService.updateDesk(id, deskDTO, token));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDesk(
            @PathVariable Long id,
            HttpServletRequest request) {
        String token = extractToken(request);
        deskService.deleteDesk(id, token);
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
