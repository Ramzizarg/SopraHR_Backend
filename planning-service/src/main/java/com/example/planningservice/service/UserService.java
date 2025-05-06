package com.example.planningservice.service;

import com.example.planningservice.client.UserClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserService {

    private final RestTemplate restTemplate;
    private final UserClient userClient;
    
    // In-memory cache for user names to minimize calls to user-service
    private final Map<Long, String> userNameCache = new HashMap<>();

    @Autowired
    public UserService(RestTemplate restTemplate, UserClient userClient) {
        this.restTemplate = restTemplate;
        this.userClient = userClient;
    }

    /**
     * Get user name by user ID
     * Returns the full name (firstName + lastName) of the user
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserNameFallback")
    public String getUserNameById(Long userId) {
        // Check cache first
        if (userNameCache.containsKey(userId)) {
            return userNameCache.get(userId);
        }
        
        try {
            // Try direct REST call as fallback for debugging, since Feign might be having auth issues
            String userServiceUrl = "http://localhost:9001/api/users/public/" + userId;
            
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(userServiceUrl, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> userData = response.getBody();
                    String firstName = userData.getOrDefault("firstName", "").toString();
                    String lastName = userData.getOrDefault("lastName", "").toString();
                    String userName = firstName + " " + lastName;
                    
                    // Cache the result
                    userNameCache.put(userId, userName);
                    return userName;
                }
            } catch (Exception e) {
                log.warn("Failed to get user data from public endpoint: {}", e.getMessage());
                // Continue to Feign client as backup
            }
            
            // Use the Feign client as backup
            try {
                // Get JWT token from security context - this part would need real implementation
                String token = "Bearer " + getServiceToken();
                
                // Use Feign client to get user details
                UserClient.UserDTO user = userClient.getUserById(userId, token);
                String userName = user.getFirstName() + " " + user.getLastName();
                
                log.info("Retrieved user name for ID {}: {}", userId, userName);
                
                // Cache the result
                userNameCache.put(userId, userName);
                return userName;
            } catch (Exception e) {
                log.error("Error with Feign client: {}", e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            log.error("Error retrieving user with ID {}: {}", userId, e.getMessage());
            return "User " + userId;
        }
    }
    
    /**
     * Fallback method for circuit breaker
     */
    public String getUserNameFallback(Long userId, Exception e) {
        log.error("Circuit breaker triggered: Could not retrieve user name for ID {}: {}", userId, e.getMessage());
        return "User " + userId + " (offline)";
    }
    
    /**
     * Get a service token for internal service-to-service communication
     * This is a simplified implementation - in production, you would implement proper service authentication
     */
    private String getServiceToken() {
        // In a real implementation, this would use client credentials flow or another service authentication method
        // For now, just return a dummy token or try to use an existing token from security context
        return "service-token-for-internal-use";
    }
}
