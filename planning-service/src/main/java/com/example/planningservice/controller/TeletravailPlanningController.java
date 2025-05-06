package com.example.planningservice.controller;

import com.example.planningservice.dto.PlanningGenerationRequest;
import com.example.planningservice.dto.PlanningResponseDTO;
import com.example.planningservice.dto.TeletravailRequestDTO;
import com.example.planningservice.service.TeletravailPlanningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/planning")
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class TeletravailPlanningController {

    private final TeletravailPlanningService planningService;

    @Autowired
    public TeletravailPlanningController(TeletravailPlanningService planningService) {
        this.planningService = planningService;
    }

    /**
     * Sync a teletravail request with planning
     * This endpoint is called by the teletravail-service when a teletravail request is created, updated, or deleted
     * This endpoint needs to be accessible without authentication for service-to-service communication
     */
    @PostMapping("/sync-teletravail")
    public ResponseEntity<String> syncTeletravailRequest(@RequestBody TeletravailRequestDTO teletravailRequest) {
        try {
            log.info("Received teletravail sync request for ID: {}, User: {}, Date: {}", 
                     teletravailRequest.getId(), teletravailRequest.getUserId(), teletravailRequest.getTeletravailDate());
            
            boolean updated = planningService.syncTeletravailWithPlanning(teletravailRequest);
            
            if (updated) {
                log.info("Successfully synchronized teletravail request {} with planning", teletravailRequest.getId());
                return ResponseEntity.ok("Planning updated successfully with teletravail request");
            } else {
                log.info("No changes needed for teletravail request {}", teletravailRequest.getId());
                return ResponseEntity.ok("No planning updates needed");
            }
        } catch (Exception e) {
            log.error("Failed to sync teletravail request with planning: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to sync teletravail with planning: " + e.getMessage());
        }
    }

    /**
     * Generate planning from teletravail requests
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<PlanningResponseDTO>> generatePlanning(@RequestBody PlanningGenerationRequest request) {
        try {
            List<PlanningResponseDTO> plannings = planningService.generatePlanning(request);
            log.info("Generated {} planning entries", plannings.size());
            return ResponseEntity.ok(plannings);
        } catch (Exception e) {
            log.error("Failed to generate planning: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate planning automatically based on rules
     */
    @PostMapping("/generate-automatic")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<PlanningResponseDTO>> generateAutomaticPlanning(
            @RequestParam(required = false) Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<PlanningResponseDTO> plannings;
            if (userId != null) {
                plannings = planningService.generateAutomaticPlanning(userId, startDate, endDate);
            } else {
                // Implement logic to generate for all users
                // For now, return an error
                return ResponseEntity.badRequest().build();
            }
            
            log.info("Generated {} automatic planning entries", plannings.size());
            return ResponseEntity.ok(plannings);
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for automatic planning generation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to generate automatic planning: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get planning for a specific user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PlanningResponseDTO>> getUserPlanning(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<PlanningResponseDTO> plannings = planningService.getUserPlanning(userId, startDate, endDate);
            log.info("Retrieved {} planning entries for user {}", plannings.size(), userId);
            return ResponseEntity.ok(plannings);
        } catch (Exception e) {
            log.error("Failed to retrieve planning for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all planning entries for a date range
     */
    @GetMapping
    public ResponseEntity<List<PlanningResponseDTO>> getAllPlanning(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<PlanningResponseDTO> plannings = planningService.getAllPlanning(startDate, endDate);
            log.info("Retrieved {} planning entries", plannings.size());
            return ResponseEntity.ok(plannings);
        } catch (Exception e) {
            log.error("Failed to retrieve planning: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update planning status
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<PlanningResponseDTO> updatePlanningStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            PlanningResponseDTO planning = planningService.updatePlanningStatus(id, status);
            log.info("Updated planning {} to status {}", id, status);
            return ResponseEntity.ok(planning);
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for planning status update: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to update planning status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete planning
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deletePlanning(@PathVariable Long id) {
        try {
            planningService.deletePlanning(id);
            log.info("Deleted planning {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for planning deletion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to delete planning: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
