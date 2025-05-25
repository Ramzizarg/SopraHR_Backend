package com.example.teletravailservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class UserClient {
    private static final Logger log = LoggerFactory.getLogger(UserClient.class);
    @Value("${user.service.url}")
    private String userServiceUrl;
    private final RestTemplate restTemplate;

    public UserClient(RestTemplate restTemplate, @Value("${user.service.url}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    /**
     * Validates a user by email and returns their user ID
     * @param email The email address to validate
     * @return The user ID if found
     * @throws IllegalArgumentException if user not found
     */
    public Long validateUserByEmail(String email) {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                userServiceUrl + "/validate-by-email/" + email,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalArgumentException("Utilisateur non trouvé : " + email);
        }
        return ((Number) response.getBody().get("id")).longValue();
    }
    
    /**
     * Gets the team name for a user
     * @param userId The user ID
     * @return The team name from the user-service or "Unknown Team" if it cannot be determined
     */
    public String getUserTeamName(Long userId) {
        try {
            // Use the public endpoint for service-to-service communication
            String url = userServiceUrl.replace("/api/users", "/api/users/public") + "/" + userId + "/team";
            
            // Simple request with no special headers needed
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("Getting team for user {} from: {}", userId, url);
            
            // Execute the request
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            // Handle the response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String team = response.getBody().trim();
                if (!team.isEmpty()) {
                    log.info("Found team '{}' for user {}", team, userId);
                    return team;
                }
            }
            
            // Default fallback if team not found
            log.warn("Could not get team for user {}", userId);
            return "Unknown Team";
        } catch (Exception e) {
            log.error("Error getting team for user {}: {}", userId, e.getMessage());
            return "Unknown Team";
        }
    }
    
    /**
     * Validates if a string is a valid team name (DEV, QA, OPS, RH)
     * @param team The team name to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidTeam(String team) {
        if (team == null || team.isEmpty()) {
            return false;
        }
        
        // Log for debugging
        log.debug("Validating team name: {}", team);
        
        // Match against the known team names - case insensitive
        String uppercaseTeam = team.toUpperCase();
        boolean isValid = uppercaseTeam.equals("DEV") || 
                        uppercaseTeam.equals("QA") || 
                        uppercaseTeam.equals("OPS") || 
                        uppercaseTeam.equals("RH");
                        
        if (!isValid) {
            log.warn("Invalid team name received: '{}'", team);
        }
        
        return isValid;
    }
    
    /**
     * Gets detailed user information
     * @param userId The user ID
     * @return Map containing user details or null if not found
     */
    public Map<String, Object> getUserDetails(Long userId) {
        try {
            log.info("Getting user details for ID {}", userId);
            
            // Try the standard public endpoint for service-to-service communication
            String url = userServiceUrl.replace("/api/users", "/api/users/public") + "/" + userId;
            log.info("Trying to get user details from: {}", url);
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Execute the request
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            // Process the response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> userDetails = response.getBody();
                log.info("Successfully retrieved user details for ID {}: {}", userId, userDetails.keySet());
                return userDetails;
            }
            
            // If that fails, try the standard endpoint without /public
            url = userServiceUrl + "/" + userId;
            log.info("First endpoint failed, trying: {}", url);
            
            response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> userDetails = response.getBody();
                log.info("Successfully retrieved user details for ID {}: {}", userId, userDetails.keySet());
                return userDetails;
            }
            
            log.warn("Could not retrieve user details for ID: {}", userId);
            return null;
            
        } catch (Exception e) {
            log.error("Error getting user details for ID {}: {}", userId, e.getMessage());
            return null;
        }
    }
    
    private Map<String, Object> tryGetUserDetails(String url) {
        log.info("Trying to get user details from URL: {}", url);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            log.info("Response status from {}: {}", url, response.getStatusCode());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> userDetails = response.getBody();
                if (userDetails != null) {
                    log.info("User details received from {}: fields={}", url, userDetails.keySet());
                    return userDetails;
                } else {
                    log.warn("{} returned 200 OK but null body", url);
                }
            } else {
                log.warn("{} returned non-success status {}", url, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling {}: {}", url, e.getMessage());
            log.debug("Full stack trace for API call to {}:", url, e);
        }
        
        return null;
    }
    
    /**
     * Gets the full name of a user by user ID
     * @param userId The user ID
     * @return The user's full name (firstName + lastName) or "Unknown User" if not found
     */
    public String getFullName(Long userId) {
        try {
            log.info("Getting full name for user ID {}", userId);
            
            // Get user details directly from the user service
            Map<String, Object> userDetails = getUserDetails(userId);
            
            if (userDetails != null) {
                // Extract first name and last name
                String firstName = (String) userDetails.getOrDefault("firstName", "");
                String lastName = (String) userDetails.getOrDefault("lastName", "");
                
                if (!firstName.isEmpty() || !lastName.isEmpty()) {
                    String fullName = (firstName + " " + lastName).trim();
                    log.info("Successfully got full name '{}' for user ID {}", fullName, userId);
                    return fullName;
                }
                
                // Fallbacks if firstName/lastName are not available
                if (userDetails.containsKey("name")) {
                    return (String) userDetails.get("name");
                }
                if (userDetails.containsKey("email")) {
                    return (String) userDetails.get("email");
                }
            }
            
            log.warn("Could not get name for user ID {}", userId);
            return "Unknown User";
            
        } catch (Exception e) {
            log.error("Error getting full name for user ID {}: {}", userId, e.getMessage());
            return "Unknown User";
        }
    }
    
    /**
     * Fallback method to get user name from the standard user service endpoint
     */
    private String getUserNameFromUserService(Long userId) {
        try {
            String url = userServiceUrl + "/" + userId;
            log.info("Trying fallback - getting user details from {}", url);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> userDetails = response.getBody();
                
                String firstName = (String) userDetails.getOrDefault("firstName", "");
                String lastName = (String) userDetails.getOrDefault("lastName", "");
                
                if (!firstName.isEmpty() || !lastName.isEmpty()) {
                    return (firstName + " " + lastName).trim();
                }
                
                // Other fallbacks
                if (userDetails.containsKey("name")) {
                    return (String) userDetails.get("name");
                }
                if (userDetails.containsKey("email")) {
                    return (String) userDetails.get("email");
                }
            }
            
            log.warn("Could not get user name from any endpoint for ID: {}", userId);
            return "Unknown User";
        } catch (Exception e) {
            log.error("Error in fallback user name lookup: {}", e.getMessage());
            return "Unknown User";
        }
    }
    
    // Team ID to name conversion is no longer needed as we're now using team names directly
    
    /**
     * Validates if a string is a valid team name (DEV, QA, OPS, RH)
     * Public method for controller use
     * @param teamName The team name to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidTeamName(String teamName) {
        return isValidTeam(teamName);
    }
    
    /**
     * Checks if a user has a specific role
     * @param email The user's email
     * @param roleName The role to check for
     * @return true if the user has the role, false otherwise
     */
    public boolean hasRole(String email, String roleName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = UriComponentsBuilder.fromUriString(userServiceUrl + "/check-role")
                    .queryParam("email", email)
                    .queryParam("role", roleName)
                    .toUriString();
            
            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );
            
            return response.getStatusCode().is2xxSuccessful() && 
                   response.getBody() != null && 
                   response.getBody();
        } catch (Exception e) {
            log.error("Error checking role for user {}: {}", email, e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if a user is a team leader
     * @param email The user's email
     * @return true if the user has the TEAM_LEADER role, false otherwise
     */
    public boolean isTeamLeader(String email) {
        return hasRole(email, "TEAM_LEADER") || hasRole(email, "ROLE_TEAM_LEADER");
    }
}