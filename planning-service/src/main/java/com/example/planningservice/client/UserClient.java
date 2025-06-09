package com.example.planningservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserClient {
    
    /**
     * Get user by ID (standard endpoint with authentication)
     */
    @GetMapping("/api/users/{userId}")
    UserResponse getUserById(@PathVariable Long userId);
    
    /**
     * Get all users from a specific team (based on team enum value like "DEV", "QA")
     * Uses the internal user service functionality
     */
    @GetMapping("/api/users/team/{teamName}")
    List<UserResponse> getUsersByTeam(@PathVariable String teamName);
    
    /**
     * Get basic user information (public endpoint for service-to-service communication)
     */
    @GetMapping("/api/users/public/{userId}")
    Map<String, Object> getUserDetails(@PathVariable Long userId);
    
    /**
     * Get a user's full name (public endpoint for service-to-service communication)
     */
    @GetMapping("/api/users/public/name/{userId}")
    String getUserFullName(@PathVariable Long userId);
    
    /**
     * Get a user's team (public endpoint for service-to-service communication)
     */
    @GetMapping("/api/users/public/{userId}/team")
    String getUserTeam(@PathVariable Long userId);
    
    /**
     * Validate user by email (get user ID from email)
     */
    @GetMapping("/api/users/validate-by-email/{email}")
    Map<String, Object> validateUserByEmail(@PathVariable String email);
    
    // DTO for user information from full user endpoints
    public static class UserResponse {
        private Long id;
        private String email;
        private String role;
        private String firstName;
        private String lastName;
        private String team;
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getFirstName() {
            return firstName;
        }
        
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
        
        public String getLastName() {
            return lastName;
        }
        
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
        
        public String getTeam() {
            return team;
        }
        
        public void setTeam(String team) {
            this.team = team;
        }
        
        /**
         * Helper method to get employee's full name
         */
        public String getEmployeeName() {
            if (firstName != null && lastName != null) {
                return firstName + " " + lastName;
            } else if (firstName != null) {
                return firstName;
            } else if (lastName != null) {
                return lastName;
            }
            return "User #" + id;
        }
    }
}
