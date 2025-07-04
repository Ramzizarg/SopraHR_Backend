package com.example.userservice.controller;

import com.example.userservice.dto.UserRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create_user")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest,
                                                   BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = getValidationErrorMessage(bindingResult);
            return ResponseEntity.badRequest()
                    .body(new UserResponse(null, null, null, null, null, null, errorMessage));
        }

        try {
            return userService.createUser(
                    userRequest.email(),
                    userRequest.password(),
                    userRequest.role(),
                    userRequest.firstName(),
                    userRequest.lastName(),
                    userRequest.team()
            ).map(user -> {
                logger.info("User created successfully: {}", user.getEmail());
                return ResponseEntity.ok(new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getTeam().name()
                ));
            }).orElseGet(() -> {
                logger.warn("User creation failed for email: {}", userRequest.email());
                return ResponseEntity.badRequest()
                        .body(new UserResponse(null, null, null, null, null, null,
                                "User creation failed: Email already exists"));
            });
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error during user creation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new UserResponse(null, null, null, null, null, null, e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during user creation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponse(null, null, null, null, null, null,
                            "Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> {
                    logger.info("User retrieved: ID {}", id);
                    return ResponseEntity.ok(new UserResponse(
                            user.getId(),
                            user.getEmail(),
                            user.getRole().name(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getTeam().name()
                    ));
                })
                .orElseGet(() -> {
                    logger.warn("User not found: ID {}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new UserResponse(null, null, null, null, null, null,
                                    "User not found"));
                });
    }
    
    /**
     * Public endpoint for internal service-to-service communication
     * This endpoint is not secured and allows other services to fetch basic user information
     * without authentication. Only meant to be used by internal services within the system.
     */
    @GetMapping("/public/{id}")
    public ResponseEntity<Map<String, Object>> getPublicUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> {
                    logger.info("User retrieved from public endpoint: ID {}", id);
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("firstName", user.getFirstName());
                    userMap.put("lastName", user.getLastName());
                    // Only expose minimal necessary information for display purposes
                    return ResponseEntity.ok(userMap);
                })
                .orElseGet(() -> {
                    logger.warn("User not found in public endpoint: ID {}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
                });
    }
    
    /**
     * Public endpoint to get just the full name of a user
     * Useful for service-to-service communication where only the display name is needed
     */
    @GetMapping("/public/name/{id}")
    public ResponseEntity<String> getPublicUserFullName(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> {
                    logger.info("User name retrieved from public endpoint: ID {}", id);
                    String fullName = user.getFirstName() + " " + user.getLastName();
                    return ResponseEntity.ok(fullName);
                })
                .orElseGet(() -> {
                    logger.warn("User not found in public endpoint for name: ID {}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User " + id);
                });
    }
    
    /**
     * Public endpoint to get a user's team
     * For service-to-service communication
     */
    @GetMapping("/public/{id}/team")
    public ResponseEntity<String> getPublicUserTeam(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> {
                    String team = user.getTeam() != null ? user.getTeam().name() : "Unknown Team";
                    return ResponseEntity.ok(team);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Unknown Team"));
    }

    @GetMapping("/validate-by-email/{email}")
    public ResponseEntity<Map<String, Object>> validateByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", user.getId());
                    response.put("email", user.getEmail());
                    logger.info("User validated: {}", email);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    logger.warn("User not found for validation: {}", email);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "User not found"));
                });
    }

    // Made publicly accessible for service-to-service communication
    // Previously restricted to admin only
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers().stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getTeam().name()
                ))
                .collect(Collectors.toList());
        logger.info("Retrieved {} users", users.size());
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get all users in a specific team
     * This is an important endpoint for the planning service to retrieve team members
     * Intentionally public for service-to-service communication
     * 
     * @param teamName The name of the team (e.g., "DEV", "QA") - case insensitive
     * @return List of users in the team
     */
    @GetMapping("/team/{teamName}")
    public ResponseEntity<List<Map<String, Object>>> getUsersByTeam(@PathVariable String teamName) {
        String normalizedTeamName = teamName.toUpperCase();
        logger.info("Retrieving users for team: {} (normalized to {})", teamName, normalizedTeamName);
        
        try {
            List<Map<String, Object>> teamMembers = userService.getUsersByTeam(normalizedTeamName).stream()
                    .map(user -> {
                        Map<String, Object> member = new HashMap<>();
                        member.put("id", user.getId());
                        member.put("firstName", user.getFirstName());
                        member.put("lastName", user.getLastName());
                        member.put("employeeName", user.getFirstName() + " " + user.getLastName());
                        member.put("email", user.getEmail());
                        member.put("team", user.getTeam().name());
                        member.put("role", user.getRole().name()); // Add role information
                        return member;
                    })
                    .collect(Collectors.toList());
            
            logger.info("Found {} users in team {}", teamMembers.size(), normalizedTeamName);
            return ResponseEntity.ok(teamMembers);
        } catch (Exception e) {
            logger.error("Error retrieving users for team {}: {}", normalizedTeamName, e.getMessage());
            return ResponseEntity.ok(new ArrayList<>()); // Return empty list on error rather than error response
        }
    }

    /**
     * Get team leaders and managers for a specific team
     * This endpoint is used by the notification service to notify appropriate leaders
     * 
     * @param teamName The name of the team (e.g., "DEV", "QA") - case insensitive
     * @return List of user IDs who are team leaders or managers in the team
     */
    @GetMapping("/public/team/{teamName}/leaders-managers")
    public ResponseEntity<List<Long>> getTeamLeadersAndManagers(@PathVariable String teamName) {
        String normalizedTeamName = teamName.toUpperCase();
        logger.info("Retrieving team leaders and managers for team: {} (normalized to {})", teamName, normalizedTeamName);
        
        try {
            List<Long> leadersAndManagers = userService.getTeamLeadersAndManagers(normalizedTeamName);
            logger.info("Found {} team leaders and managers for team {}", leadersAndManagers.size(), normalizedTeamName);
            return ResponseEntity.ok(leadersAndManagers);
        } catch (Exception e) {
            logger.error("Error retrieving team leaders and managers for team {}: {}", normalizedTeamName, e.getMessage());
            return ResponseEntity.ok(new ArrayList<>()); // Return empty list on error
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UserRequest userRequest,
                                                   BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = getValidationErrorMessage(bindingResult);
            return ResponseEntity.badRequest()
                    .body(new UserResponse(null, null, null, null, null, null, errorMessage));
        }

        try {
            return userService.updateUser(
                    id,
                    userRequest.email(),
                    userRequest.password(),
                    userRequest.role(),
                    userRequest.firstName(),
                    userRequest.lastName(),
                    userRequest.team()
            ).map(user -> {
                logger.info("User updated successfully: ID {}", id);
                return ResponseEntity.ok(new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getTeam().name()
                ));
            }).orElseGet(() -> {
                logger.warn("User not found for update: ID {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new UserResponse(null, null, null, null, null, null,
                                "User not found"));
            });
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error during user update: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new UserResponse(null, null, null, null, null, null, e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during user update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponse(null, null, null, null, null, null,
                            "Internal server error: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<UserResponse> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            logger.info("User deleted successfully: ID {}", id);
            return ResponseEntity.ok(new UserResponse(id, null, null, null, null, null,
                    "User deleted successfully"));
        } else {
            logger.warn("User not found for deletion: ID {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponse(null, null, null, null, null, null,
                            "User not found"));
        }
    }

    private String getValidationErrorMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
    }
}