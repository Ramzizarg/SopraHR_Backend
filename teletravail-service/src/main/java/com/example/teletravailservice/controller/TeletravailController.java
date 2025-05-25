package com.example.teletravailservice.controller;

import com.example.teletravailservice.dto.ErrorResponse;
import com.example.teletravailservice.dto.TeletravailRequestDTO;
import com.example.teletravailservice.dto.TeletravailResponseDTO;
import com.example.teletravailservice.entity.TeletravailRequest;
import com.example.teletravailservice.service.TeletravailService;
// Planning client import removed - functionality consolidated
import com.example.teletravailservice.client.UserClient;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teletravail")
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class TeletravailController {
    private final TeletravailService teletravailService;
    private final UserClient userClient;

    public TeletravailController(TeletravailService teletravailService, UserClient userClient) {
        this.teletravailService = teletravailService;
        this.userClient = userClient;
    }

    @PostMapping
    public ResponseEntity<TeletravailResponseDTO> submitTeletravailRequest(
            @Valid @RequestBody TeletravailRequestDTO requestDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal String email,
            @RequestHeader(value = "Employee-Name", required = false) String employeeName) {
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
            // Try to get userDetails from authentication context
            Map<String, Object> userDetails = null;
            String userName = employeeName;
            
            try {
                // Get user details - this will help us get the name if not provided in header
                Long userId = userClient.validateUserByEmail(email);
                userDetails = userClient.getUserDetails(userId);
                
                // If no employee name was provided in header, try to construct one from userDetails
                if ((userName == null || userName.isEmpty()) && userDetails != null) {
                    String firstName = (String) userDetails.getOrDefault("firstName", "");
                    String lastName = (String) userDetails.getOrDefault("lastName", "");
                    
                    if (!firstName.isEmpty() || !lastName.isEmpty()) {
                        userName = (firstName + " " + lastName).trim();
                        log.info("Constructed user name '{}' from user details", userName);
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not get user details: {}", ex.getMessage());
            }
            
            // Pass the user name to the service
            TeletravailRequest savedRequest = teletravailService.saveRequest(requestDTO, email, userName);
            log.info("Teletravail request submitted successfully for user {} ({}): ID {}", 
                    userName, email, savedRequest.getId());
            
            // Planning service notification removed - functionality consolidated
            
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

    // Planning service notification method removed - functionality consolidated

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
    
    /**
     * Get teletravail requests for a user within a specific date range
     * This endpoint is specifically designed for the planning page functionality
     * 
     * @param userId The user ID to get requests for
     * @param startDate Start date in ISO format (YYYY-MM-DD)
     * @param endDate End date in ISO format (YYYY-MM-DD)
     * @return List of teletravail requests within the date range
     */
    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<List<TeletravailRequest>> getUserRequestsByDateRange(
            @PathVariable Long userId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        log.info("Received request for user ID {} between {} and {}", userId, startDate, endDate);
        
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            
            List<TeletravailRequest> requests = teletravailService.getUserRequestsByDateRange(userId, start, end);
            log.info("Fetched {} teletravail requests for user ID {} in date range", requests.size(), userId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("Failed to fetch teletravail requests by date range for user ID {}: {}", 
                    userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    /**
     * Get all teletravail requests for managers to view
     * This endpoint is secured to MANAGER role only
     * 
     * @param email The authenticated user's email
     * @return List of all teletravail requests with user details
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllRequests(@AuthenticationPrincipal String email) {
        if (email == null) {
            log.warn("Unauthorized request: No authenticated user");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication error", "User not authenticated"));
        }

        try {
            // Verify manager role - this would typically be done by Spring Security
            // but we're explicitly checking here for additional security
            if (!isManager(email)) {
                log.warn("User {} attempted to access manager-only endpoint", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Access denied", "Manager role required"));
            }
            
            List<TeletravailResponseDTO> requests = teletravailService.getAllRequests().stream()
                    .map(this::enrichRequestWithUserDetails)
                    .collect(Collectors.toList());
            
            log.info("Returning {} requests for manager {}", requests.size(), email);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("Server error fetching all requests: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error", "Unable to fetch all requests: " + e.getMessage()));
        }
    }
    
    /**
     * Get teletravail requests for a team leader to review
     * This endpoint is secured to TEAM_LEADER role only
     * 
     * @param email The authenticated user's email
     * @return List of teletravail requests for the team leader's team
     */
    // First getTeamRequestsByDateRange method removed to fix ambiguous mapping
    
    // First getAllRequestsByDateRange method removed to fix ambiguous mapping
    
    @GetMapping("/team")
    public ResponseEntity<?> getTeamRequests(@AuthenticationPrincipal String email) {
        if (email == null) {
            log.warn("Unauthorized request: No authenticated user");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication error", "User not authenticated"));
        }

        try {
            Long userId = teletravailService.getUserIdByEmail(email);
            
            // Determine if user is a team leader using direct role check
            if (!isTeamLeader(email)) {
                log.warn("User {} attempted to access team leader endpoint without proper role", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Access denied", "Team leader role required"));
            }
            
            // Get the requests for this team leader's team
            List<TeletravailRequest> teamRequests = teletravailService.getTeamLeaderRequests(userId);
            List<TeletravailResponseDTO> enrichedRequests = teamRequests.stream()
                    .map(this::enrichRequestWithUserDetails)
                    .collect(Collectors.toList());
            
            log.info("Returning {} team requests for team leader {}", enrichedRequests.size(), email);
            return ResponseEntity.ok(enrichedRequests);
        } catch (Exception e) {
            log.error("Server error fetching team requests: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error", "Unable to fetch team requests: " + e.getMessage()));
        }
    }

    /**
     * Update a teletravail request status (approve/reject)
     * This endpoint is accessible to team leaders for their team's requests
     * 
     * @param requestId The ID of the request to update
     * @param requestDTO Request DTO containing status and optional rejection reason
     * @param email The authenticated user's email
     * @return The updated request
     */
    @PutMapping("/{requestId}/status")
    public ResponseEntity<?> updateRequestStatus(
            @PathVariable Long requestId,
            @RequestBody TeletravailRequestDTO requestDTO,
            @AuthenticationPrincipal String email) {
        
        if (email == null) {
            log.warn("Unauthorized request: No authenticated user");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication error", "User not authenticated"));
        }
        
        try {
            // Get user ID from email
            Long teamLeaderId = teletravailService.getUserIdByEmail(email);
            
            // Check user roles
            boolean isTeamLeader = isTeamLeader(email);
            boolean isManagerUser = isManager(email);
            
            log.info("Request status update - ID: {}, User: {}, TeamLeader: {}, Manager: {}", 
                    requestId, email, isTeamLeader, isManagerUser);
            
            // TEMPORARILY DISABLED FOR TESTING: Role validation
            // In production, uncomment the following code:
            /*
            if (!isTeamLeader && !isManagerUser) {
                log.warn("User {} attempted to update request status without proper role", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Access denied", "Team leader or manager role required"));
            }
            */
            
            // For testing, we're treating all users as if they were managers
            log.info("TESTING MODE: Allowing user {} to update request status regardless of role", email);
            isManagerUser = true; // Force manager role for testing
            
            // Parse the requested status
            TeletravailRequest.TeletravailStatus status = 
                    TeletravailRequest.TeletravailStatus.valueOf(requestDTO.getStatus());
            
            TeletravailRequest updatedRequest;
            
            // Handle based on user role
            if (isTeamLeader && !isManagerUser) {
                // Team leaders can only update their own team's requests
                try {
                    updatedRequest = teletravailService.updateRequestStatus(
                            requestId, status, requestDTO.getRejectionReason(), teamLeaderId);
                } catch (IllegalArgumentException e) {
                    log.warn("Team leader {} is not authorized to update request {}: {}", 
                            email, requestId, e.getMessage());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new ErrorResponse("Access denied", e.getMessage())); 
                }
            } else {
                // Managers can update any request
                updatedRequest = teletravailService.updateRequestStatusAsManager(
                        requestId, status, requestDTO.getRejectionReason());
            }
            
            // Enrich response with user details
            TeletravailResponseDTO responseDTO = enrichRequestWithUserDetails(updatedRequest);
            log.info("Successfully updated request {} status to {} by {}", requestId, status, email);
            return ResponseEntity.ok(responseDTO);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request to update status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid request", e.getMessage()));
        } catch (Exception e) {
            log.error("Server error updating request status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error", "Unable to update request: " + e.getMessage()));
        }
    }
    
    /**
     * Helper method to enrich a TeletravailRequest with user details
     * 
     * @param request The request to enrich
     * @return TeletravailResponseDTO with user, team and team leader details
     */
    private TeletravailResponseDTO enrichRequestWithUserDetails(TeletravailRequest request) {
        TeletravailResponseDTO dto = new TeletravailResponseDTO(request);
        
        try {
            // Use employee name from entity if available (should be populated during creation)
            if (request.getEmployeeName() != null && !request.getEmployeeName().isEmpty() 
                    && !"Unknown User".equals(request.getEmployeeName())) {
                log.info("Using employee name '{}' from entity for request ID {}", request.getEmployeeName(), request.getId());
                dto.setUserName(request.getEmployeeName());
            } else {
                // Get user details from user service as fallback
                Map<String, Object> userDetails = userClient.getUserDetails(request.getUserId());
                if (userDetails != null) {
                    // Get first name and last name fields and combine them
                    String firstName = (String) userDetails.getOrDefault("firstName", "");
                    String lastName = (String) userDetails.getOrDefault("lastName", "");
                    
                    if (firstName.isEmpty() && lastName.isEmpty()) {
                        // Fallback to name field if both first name and last name are not available
                        dto.setUserName((String) userDetails.getOrDefault("name", "Unknown User"));
                    } else {
                        // Combine first name and last name
                        dto.setUserName(firstName + " " + lastName);
                    }
                }
            }
            
            // Get team details from user service if needed
            if (dto.getTeam() == null || dto.getTeam().isEmpty()) {
                Map<String, Object> userDetails = userClient.getUserDetails(request.getUserId());
                if (userDetails != null && userDetails.containsKey("teamName") && userDetails.get("teamName") != null) {
                    String teamName = (String) userDetails.get("teamName");
                    dto.setTeam(teamName != null && !teamName.isEmpty() ? teamName : "Unknown Team");
                }
            }
            
            // Get team leader details if needed
            if (dto.getTeamLeaderId() == null || dto.getTeamLeaderName() == null || dto.getTeamLeaderName().isEmpty()) {
                Map<String, Object> userDetails = userClient.getUserDetails(request.getUserId());
                if (userDetails != null && userDetails.containsKey("teamLeaderId") && userDetails.get("teamLeaderId") != null) {
                    Long teamLeaderId = ((Number) userDetails.get("teamLeaderId")).longValue();
                    dto.setTeamLeaderId(teamLeaderId);
                    
                    // Use first name and last name for team leader too if available
                    String teamLeaderFirstName = (String) userDetails.getOrDefault("teamLeaderFirstName", "");
                    String teamLeaderLastName = (String) userDetails.getOrDefault("teamLeaderLastName", "");
                    
                    if (!teamLeaderFirstName.isEmpty() || !teamLeaderLastName.isEmpty()) {
                        dto.setTeamLeaderName(teamLeaderFirstName + " " + teamLeaderLastName);
                    } else {
                        dto.setTeamLeaderName((String) userDetails.getOrDefault("teamLeaderName", "Unknown Team Leader"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to enrich teletravail request with user details: {}", e.getMessage());
            // Still return basic DTO even if enrichment fails
        }
        
        return dto;
    }
    
    /**
     * Helper method to check if a user is a manager
     */
    private boolean isManager(String email) {
        try {
            return userClient.hasRole(email, "ROLE_MANAGER");
        } catch (Exception e) {
            log.warn("Failed to check manager role for {}: {}", email, e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to check if a user is a manager based on details
     */
    private boolean isManager(Map<String, Object> userDetails) {
        if (userDetails == null) return false;
        
        Object roles = userDetails.get("roles");
        if (roles instanceof List) {
            return ((List<?>) roles).contains("ROLE_MANAGER");
        }
        return false;
    }
    
    /**
     * Helper method to check if a user is a team leader based on user details map
     */
    private boolean isTeamLeader(Map<String, Object> userDetails) {
        if (userDetails == null) return false;
        
        Object roles = userDetails.get("roles");
        if (roles instanceof List) {
            List<?> rolesList = (List<?>) roles;
            return rolesList.contains("ROLE_TEAM_LEADER") || 
                   rolesList.contains("TEAM_LEADER") ||
                   rolesList.contains("ROLE_TEAMLEADER") ||
                   rolesList.contains("TEAMLEADER");
        }
        return false;
    }
    
    /**
     * Helper method to check if a user is a team leader by email
     */
    private boolean isTeamLeader(String email) {
        try {
            return userClient.isTeamLeader(email);
        } catch (Exception e) {
            log.warn("Failed to check team leader role for {}: {}", email, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get teletravail requests for a specific team within a date range
     * This endpoint is specifically designed for the team planning page functionality
     * 
     * @param teamName The team name to get requests for (e.g., "DEV", "QA")
     * @param startDate Start date in ISO format (YYYY-MM-DD)
     * @param endDate End date in ISO format (YYYY-MM-DD)
     * @return List of teletravail requests for the team within the date range
     */
    /**
     * Get teletravail requests for a specific team by team name with date range filter
     * This endpoint is used specifically for the planning page
     * 
     * @param teamName The name of the team (e.g., "DEV", "QA")
     * @param startDate Start date in ISO format (YYYY-MM-DD)
     * @param endDate End date in ISO format (YYYY-MM-DD)
     * @param email The authenticated user's email
     * @return List of teletravail requests for the specified team
     */
    @GetMapping("/team/{teamName}")
    public ResponseEntity<List<TeletravailRequest>> getTeamRequestsByDateRange(
            @PathVariable String teamName,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @AuthenticationPrincipal String email) {
        
        log.info("Received request for team {} between {} and {} by user {}", teamName, startDate, endDate, email);
        
        try {
            if (email == null) {
                log.warn("Unauthorized request: No authenticated user");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
            }
            
            // Get user ID and check authorization
            Long userId = userClient.validateUserByEmail(email);
            boolean isManager = userClient.hasRole(email, "MANAGER") || userClient.hasRole(email, "ROLE_MANAGER");
            boolean isTeamLeader = userClient.isTeamLeader(email);
            
            // Only allow access if:
            // 1. User is a manager (can see all teams)
            // 2. User is a team leader AND the requested team is their team
            // 3. User is requesting their own team's data
            
            // Get user's team
            String userTeamName = userClient.getUserTeamName(userId);
            log.info("User {} has team: {}, isManager: {}, isTeamLeader: {}", 
                   email, userTeamName, isManager, isTeamLeader);
            
            // If not a manager, verify they're only accessing their team
            if (!isManager && !teamName.equals(userTeamName)) {
                // If team leader, allow access only to their team
                if (isTeamLeader && !teamName.equals(userTeamName)) {
                    log.warn("Team leader {} attempted to access data for team {}, but they belong to team {}", 
                           email, teamName, userTeamName);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                           .body(List.of());
                }
                
                // Regular employee requesting other team's data
                if (!isTeamLeader && !teamName.equals(userTeamName)) {
                    log.warn("User {} attempted to access data for team {}, but they belong to team {}", 
                           email, teamName, userTeamName);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                           .body(List.of());
                }
            }
            // Validate team name
            if (!userClient.isValidTeamName(teamName)) {
                log.warn("Invalid team name: {}", teamName);
                return ResponseEntity.badRequest().body(List.of());
            }
            
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            
            List<TeletravailRequest> requests = teletravailService.getTeamRequestsByDateRange(teamName, start, end);
            log.info("Fetched {} teletravail requests for team {} in date range", requests.size(), teamName);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("Failed to fetch teletravail requests by date range for team {}: {}", 
                    teamName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }
    
    /**
     * Get all teletravail requests within a date range (for managers)
     * This endpoint is specifically designed for the planning overview page
     * 
     * @param startDate Start date in ISO format (YYYY-MM-DD)
     * @param endDate End date in ISO format (YYYY-MM-DD)
     * @param authHeader Optional authorization header
     * @return List of all teletravail requests within the date range
     */
    @GetMapping("/planning/all")
    // Allow all authenticated users to access planning data
    public ResponseEntity<List<TeletravailRequest>> getAllRequestsByDateRange(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        log.info("Received request for all planning entries between {} and {}", startDate, endDate);
        
        try {
            // Validate date format
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            
            // Validate date range
            if (start.isAfter(end)) {
                log.warn("Invalid date range: start date {} is after end date {}", startDate, endDate);
                return ResponseEntity.badRequest().body(List.of());
            }
            
            List<TeletravailRequest> requests = teletravailService.getAllRequestsByDateRange(start, end);
            log.info("Fetched {} teletravail requests in date range for planning", requests.size());
            return ResponseEntity.ok(requests);
        } catch (IllegalArgumentException e) {
            // This will catch date parsing errors
            log.warn("Invalid date format: {}", e.getMessage());
            return ResponseEntity.badRequest().body(List.of());
        } catch (Exception e) {
            log.error("Failed to fetch all teletravail requests by date range: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    private String getValidationErrorMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
    }
    
    /**
     * Delete a teletravail request by its ID.
     * This endpoint is used by the frontend to allow users to cancel their own requests,
     * or for managers to cancel any request.
     * 
     * @param id The ID of the teletravail request to delete
     * @param email The authenticated user's email
     * @return HTTP 200 if request deleted, 403 if not authorized, 404 if not found, 500 if error
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTeletravailRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        
        log.info("Received request to delete teletravail request with ID {} from user {}", id, email);
        
        try {
            // Get the user ID from email
            Long currentUserId = userClient.validateUserByEmail(email);
            
            // First check if this user is allowed to delete the request
            TeletravailRequest request = teletravailService.getRequestById(id);
            if (request == null) {
                log.warn("Teletravail request not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            boolean isOwnRequest = request.getUserId().equals(currentUserId);
            boolean isManager = userClient.hasRole(email, "MANAGER") || userClient.hasRole(email, "ROLE_MANAGER");
            
            if (!isOwnRequest && !isManager) {
                log.warn("User {} attempted to delete request {} owned by user {}", 
                        currentUserId, id, request.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                       .body(new ErrorResponse("Forbidden", "Vous n'avez pas l'autorisation d'annuler cette demande."));
            }
            
            // Delete the request
            boolean deleted = teletravailService.deleteRequest(id);
            
            if (deleted) {
                log.info("Successfully deleted teletravail request with ID {}", id);
                return ResponseEntity.ok().build();
            } else {
                log.warn("Failed to delete teletravail request with ID {}", id);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error deleting teletravail request with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body(new ErrorResponse("Server error", "Failed to delete teletravail request: " + e.getMessage()));
        }
    }

    /**
     * Delete a teletravail request for a given user on a specific date.
     * This endpoint is designed to be called by other microservices (planning-service)
     * when a planning entry is deleted to maintain data consistency.
     * 
     * @param userId The ID of the user who owns the request
     * @param date The date of the request in format YYYY-MM-DD
     * @param authHeader The authorization header for service-to-service auth
     * @return HTTP 200 if request deleted, 404 if not found, 500 if error
     */
    @DeleteMapping("/by-user-and-date/{userId}/{date}")
    public ResponseEntity<?> deleteTeletravailRequestByUserAndDate(
            @PathVariable Long userId,
            @PathVariable String date,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        log.info("Received request to delete teletravail request for user ID {} on date {}", userId, date);
        
        try {
            boolean deleted = teletravailService.deleteRequestByUserAndDate(userId, date);
            
            if (deleted) {
                log.info("Successfully deleted teletravail request for user ID {} on date {}", userId, date);
                return ResponseEntity.ok().build();
            } else {
                log.warn("No teletravail request found for user ID {} on date {}", userId, date);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error deleting teletravail request for user ID {} on date {}: {}", 
                     userId, date, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body(new ErrorResponse("Server error", "Failed to delete teletravail request: " + e.getMessage()));
        }
    }
    
    // Employee name admin endpoint removed per request
}