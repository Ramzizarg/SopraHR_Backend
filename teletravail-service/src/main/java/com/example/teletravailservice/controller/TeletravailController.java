package com.example.teletravailservice.controller;

import com.example.teletravailservice.dto.ErrorResponse;
import com.example.teletravailservice.dto.TeletravailRequestDTO;
import com.example.teletravailservice.dto.TeletravailResponseDTO;
import com.example.teletravailservice.entity.TeletravailRequest;
import com.example.teletravailservice.service.TeletravailService;
import com.example.teletravailservice.client.PlanningClient;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teletravail")
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST})
public class TeletravailController {
    private final TeletravailService teletravailService;
    private final PlanningClient planningClient;

    public TeletravailController(TeletravailService teletravailService, PlanningClient planningClient) {
        this.teletravailService = teletravailService;
        this.planningClient = planningClient;
    }

    @PostMapping
    public ResponseEntity<TeletravailResponseDTO> submitTeletravailRequest(
            @Valid @RequestBody TeletravailRequestDTO requestDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal String email) {
        if (bindingResult.hasErrors()) {
            String errorMessage = getValidationErrorMessage(bindingResult);
            log.warn("Validation error: {}", errorMessage);
            return ResponseEntity.badRequest().body(new TeletravailResponseDTO(errorMessage));
        }

        if (email == null) {
            log.warn("Unauthorized request: No authenticated user");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TeletravailResponseDTO("Unauthorized: User not authenticated"));
        }

        try {
            TeletravailRequest savedRequest = teletravailService.saveRequest(requestDTO, email);
            log.info("Teletravail request submitted successfully for user {}: ID {}", email, savedRequest.getId());
            
            // Notify planning service of the new teletravail request
            notifyPlanningService(savedRequest);
            
            return ResponseEntity.ok(new TeletravailResponseDTO(savedRequest));
        } catch (IllegalArgumentException e) {
            log.warn("Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new TeletravailResponseDTO(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to submit teletravail request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TeletravailResponseDTO("Server error: " + e.getMessage()));
        }
    }

    /**
     * Notify the planning service of changes to teletravail requests
     * @param request The teletravail request that was created/updated
     */
    private void notifyPlanningService(TeletravailRequest request) {
        try {
            log.info("Notifying planning service of teletravail request ID: {}", request.getId());
            planningClient.syncTeletravailRequest(request);
        } catch (Exception e) {
            // Log but don't fail the main operation if notification fails
            log.error("Failed to notify planning service: {}", e.getMessage(), e);
        }
    }

    @GetMapping("/countries")
    public ResponseEntity<?> getCountries() {
        try {
            List<String> countries = teletravailService.getAllCountries();
            log.info("Fetched {} countries successfully", countries.size());
            return ResponseEntity.ok(countries);
        } catch (Exception e) {
            log.error("Failed to fetch countries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error", "Unable to fetch countries: " + e.getMessage()));
        }
    }

    @GetMapping("/regions/{countryName}")
    public ResponseEntity<?> getRegions(@PathVariable String countryName) {
        try {
            if (countryName == null || countryName.trim().isEmpty()) {
                log.warn("Invalid countryName provided: {}", countryName);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid request", "Country name is required"));
            }
            List<String> regions = teletravailService.getRegionsByCountry(countryName);
            log.info("Fetched {} regions for country '{}'", regions.size(), countryName);
            return ResponseEntity.ok(regions);
        } catch (Exception e) {
            log.error("Failed to fetch regions for country '{}': {}", countryName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error", "Unable to fetch regions: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserRequests(@AuthenticationPrincipal String email) {
        if (email == null) {
            log.warn("Unauthorized request: No authenticated user");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication error", "User not authenticated"));
        }

        try {
            Long userId = teletravailService.getUserIdByEmail(email); // Assuming UserClient is used here
            List<TeletravailResponseDTO> requests = teletravailService.getUserRequests(userId).stream()
                    .map(TeletravailResponseDTO::new)
                    .collect(Collectors.toList());
            log.info("Returning {} requests for user {}", requests.size(), email);
            return ResponseEntity.ok(requests);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to fetch requests for user {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Invalid request", e.getMessage()));
        } catch (Exception e) {
            log.error("Server error fetching requests for user {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error", "Unable to fetch user requests: " + e.getMessage()));
        }
    }
    
    /**
     * Get all teletravail requests for a specific user by user ID.
     * This endpoint is used by other microservices via Feign clients.
     * 
     * @param userId The ID of the user to get requests for
     * @return List of teletravail requests for the user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TeletravailRequest>> getUserRequestsById(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        log.info("Received request for user ID {} with auth token: {}", userId, 
                authHeader != null ? "present" : "missing");
        
        try {
            List<TeletravailRequest> userRequests = teletravailService.getUserRequests(userId);
            log.info("Fetched {} teletravail requests for user ID {}", userRequests.size(), userId);
            return ResponseEntity.ok(userRequests);
        } catch (Exception e) {
            log.error("Failed to fetch teletravail requests for user ID {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    private String getValidationErrorMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
    }
}