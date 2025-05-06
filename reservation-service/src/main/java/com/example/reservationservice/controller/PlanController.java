package com.example.reservationservice.controller;

import com.example.reservationservice.dto.PlanDTO;
import com.example.reservationservice.exception.BadRequestException;
import com.example.reservationservice.exception.ResourceNotFoundException;
import com.example.reservationservice.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/plans")
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class PlanController {

    private static final Logger logger = LoggerFactory.getLogger(PlanController.class);
    private final PlanService planService;

    @Autowired
    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<List<PlanDTO>> getAllPlans() {
        logger.info("Request received to get all plans");
        List<PlanDTO> plans = planService.getAllPlans();
        
        if (plans.isEmpty()) {
            logger.info("No plans found");
            return ResponseEntity.noContent().build();
        }
        
        logger.info("Returning {} plans", plans.size());
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanDTO> getPlanById(@PathVariable Long id) {
        logger.info("Request received to get plan with ID: {}", id);
        try {
            PlanDTO plan = planService.getPlanById(id);
            logger.info("Successfully retrieved plan with ID: {}", id);
            return ResponseEntity.ok(plan);
        } catch (ResourceNotFoundException e) {
            logger.warn("Plan not found with ID: {}", id);
            throw e;
        }
    }
    
    @GetMapping("/plan-id/{planId}")
    public ResponseEntity<PlanDTO> getPlanByPlanId(@PathVariable String planId) {
        logger.info("Request received to get plan with plan ID: {}", planId);
        try {
            PlanDTO plan = planService.getPlanByPlanId(planId);
            logger.info("Successfully retrieved plan with plan ID: {}", planId);
            return ResponseEntity.ok(plan);
        } catch (ResourceNotFoundException e) {
            logger.warn("Plan not found with plan ID: {}", planId);
            throw e;
        }
    }
    
    @GetMapping("/{id}/availability")
    public ResponseEntity<PlanDTO> getPlanWithAvailabilityByDate(
            @PathVariable Long id,
            @RequestParam String date) {
        logger.info("Request received to get plan with ID: {} and availability for date: {}", id, date);
        try {
            PlanDTO plan = planService.getPlanWithAvailabilityByDate(id, date);
            logger.info("Successfully retrieved plan with availability for date: {}", date);
            return ResponseEntity.ok(plan);
        } catch (ResourceNotFoundException e) {
            logger.warn("Plan not found with ID: {}", id);
            throw e;
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid date format: {}", date);
            throw new BadRequestException("Invalid date format. Expected format: yyyy-MM-dd");
        }
    }

    @PostMapping
    public ResponseEntity<PlanDTO> createPlan(
            @RequestBody PlanDTO planDTO,
            HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            logger.warn("Attempt to create plan without authentication token");
            throw new BadRequestException("Authentication token is required");
        }
        
        logger.info("Request received to create a new plan");
        try {
            PlanDTO createdPlan = planService.createPlan(planDTO, token);
            logger.info("Plan created successfully with ID: {}", createdPlan.getId());
            return new ResponseEntity<>(createdPlan, HttpStatus.CREATED);
        } catch (BadRequestException e) {
            logger.warn("Failed to create plan: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error creating plan: {}", e.getMessage());
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanDTO> updatePlan(
            @PathVariable Long id,
            @RequestBody PlanDTO planDTO,
            HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            logger.warn("Attempt to update plan without authentication token");
            throw new BadRequestException("Authentication token is required");
        }
        
        logger.info("Request received to update plan with ID: {}", id);
        try {
            PlanDTO updatedPlan = planService.updatePlan(id, planDTO, token);
            logger.info("Plan with ID: {} updated successfully", id);
            return ResponseEntity.ok(updatedPlan);
        } catch (ResourceNotFoundException e) {
            logger.warn("Plan not found with ID: {}", id);
            throw e;
        } catch (Exception e) {
            logger.error("Error updating plan: {}", e.getMessage());
            throw e;
        }
    }
    
    @PutMapping("/{id}/full")
    public ResponseEntity<PlanDTO> updateFullPlan(
            @PathVariable Long id,
            @RequestBody PlanDTO planDTO,
            HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            logger.warn("Attempt to update full plan without authentication token");
            throw new BadRequestException("Authentication token is required");
        }
        
        logger.info("Request received to update full plan with ID: {}", id);
        try {
            PlanDTO updatedPlan = planService.updateFullPlan(id, planDTO, token);
            logger.info("Full plan with ID: {} updated successfully with {} desks and {} walls", 
                      id, planDTO.getDesks().size(), planDTO.getWalls().size());
            return ResponseEntity.ok(updatedPlan);
        } catch (ResourceNotFoundException e) {
            logger.warn("Plan not found with ID: {}", id);
            throw e;
        } catch (Exception e) {
            logger.error("Error updating full plan: {}", e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletePlan(
            @PathVariable Long id,
            HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            logger.warn("Attempt to delete plan without authentication token");
            throw new BadRequestException("Authentication token is required");
        }
        
        logger.info("Request received to delete plan with ID: {}", id);
        try {
            planService.deletePlan(id, token);
            logger.info("Plan with ID: {} deleted successfully", id);
            
            // Return a success message instead of empty response
            Map<String, String> response = new HashMap<>();
            response.put("message", "Plan deleted successfully");
            response.put("planId", id.toString());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            logger.warn("Plan not found with ID: {}", id);
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting plan: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Extracts the JWT token from the Authorization header
     * 
     * @param request The HTTP request containing the Authorization header
     * @return The JWT token without the "Bearer " prefix, or null if not found
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            logger.debug("JWT token extracted from request");
            return token;
        }
        logger.warn("No valid authorization token found in request");
        return null;
    }
}
