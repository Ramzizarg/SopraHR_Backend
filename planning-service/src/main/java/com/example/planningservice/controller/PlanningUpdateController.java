package com.example.planningservice.controller;

import com.example.planningservice.service.TeletravailPlanningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/planning")
public class PlanningUpdateController {
    private static final Logger logger = LoggerFactory.getLogger(PlanningUpdateController.class);

    private final TeletravailPlanningService planningService;

    @Autowired
    public PlanningUpdateController(TeletravailPlanningService planningService) {
        this.planningService = planningService;
    }

    /**
     * Updates planning for a specific user when a telework request is added.
     * This endpoint is called automatically by the teletravail-service.
     *
     * @param userId The ID of the user whose planning should be updated
     * @return Response with success or error message
     */
    @PostMapping("/update-for-user/{userId}")
    public ResponseEntity<String> updatePlanningForUser(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        logger.info("Received request to update planning for user ID: {} with auth token: {}", 
                userId, authHeader != null ? "present" : "missing");
        
        try {
            // Get the current month's date range
            LocalDate startDate = LocalDate.now().withDayOfMonth(1);
            LocalDate endDate = startDate.plusMonths(1).minusDays(1);
            
            // Fetch current planning or generate if not exists
            boolean planningExists = planningService.checkIfPlanningExistsForUser(userId, startDate, endDate);
            
            if (!planningExists) {
                logger.info("No planning exists for user {}, generating new planning", userId);
                planningService.generatePlanningForUser(userId, startDate, endDate, authHeader);
            } else {
                logger.info("Planning exists for user {}, updating existing planning", userId);
                planningService.updatePlanningForUser(userId, startDate, endDate, authHeader);
            }
            
            return ResponseEntity.ok("Planning updated successfully for user " + userId);
        } catch (Exception e) {
            logger.error("Error updating planning for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error updating planning: " + e.getMessage());
        }
    }
}
