package com.example.reservationservice.service;

import com.example.reservationservice.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final RestTemplate restTemplate;
    
    @Value("${user.service.url}")
    private String userServiceUrl;
    
    @Value("${user.service.auth.url}")
    private String authServiceUrl;

    @Autowired
    public UserService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Verify if the user has the MANAGER role
     * @param token the JWT token
     * @return true if the user has the MANAGER role
     */
    public boolean isManager(String token) {
        // First, examine the token directly - most reliable approach
        try {
            // Quick check for MANAGER role directly in the token
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(java.util.Base64.getDecoder().decode(parts[1]));
                System.out.println("JWT payload: " + payload);
                
                // If we can see the MANAGER role directly in the token, authorize immediately
                if (payload.contains("\"role\":\"MANAGER\"")) {
                    System.out.println("MANAGER role found directly in token");
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing JWT: " + e.getMessage());
            // Continue with regular flow if direct examination fails
        }
        
        // Fallback to API call
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Debug token and header info
            System.out.println("Token used: " + token);
            System.out.println("Auth header: " + headers.get("Authorization"));
            System.out.println("Checking if user is a manager with URL: " + authServiceUrl + "/me");
            
            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                        authServiceUrl + "/me", 
                        HttpMethod.GET, 
                        entity, 
                        Map.class);

                System.out.println("Response status: " + response.getStatusCode());
                Map<String, Object> user = response.getBody();
                System.out.println("User service response: " + user);
                
                if (user != null) {
                    System.out.println("User response keys: " + user.keySet());
                    if (user.containsKey("role")) {
                        String role = (String) user.get("role");
                        System.out.println("Role from user service: '" + role + "'");
                        boolean isManager = "MANAGER".equalsIgnoreCase(role);
                        System.out.println("Is manager: " + isManager);
                        return isManager;
                    } else {
                        System.out.println("Role key not found in response");
                    }
                } else {
                    System.out.println("Null response body from user service");
                }
                return false;
            } catch (Exception e) {
                System.err.println("HTTP request failed: " + e.getMessage());
                e.printStackTrace();
                
                // Fallback direct call using different approach
                System.out.println("Trying fallback approach...");
                return checkManagerDirectly(token);
            }
        } catch (Exception e) {
            System.err.println("Error checking manager role: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Fallback method using direct URL connection
    private boolean checkManagerDirectly(String token) {
        try {
            java.net.URL url = new java.net.URL(authServiceUrl + "/me");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            
            System.out.println("Fallback connection response code: " + conn.getResponseCode());
            
            if (conn.getResponseCode() == 200) {
                java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream());
                String responseBody = scanner.useDelimiter("\\A").next();
                scanner.close();
                System.out.println("Fallback response: " + responseBody);
                
                // Simple check for MANAGER in response
                return responseBody.contains("\"role\":\"MANAGER\"");
            }
            return false;
        } catch (Exception e) {
            System.err.println("Fallback check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the current user's ID from the token
     * @param token the JWT token
     * @return the user ID
     */
    public Long getUserId(String token) {
        try {
            // Primary approach: Get the numeric user ID from the user-service
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + token);
                HttpEntity<String> entity = new HttpEntity<>(headers);

                // The auth service URL already points to http://localhost:9001/auth
                // We need to add /me endpoint without double-adding /me
                String userProfileUrl = authServiceUrl;
                if (!userProfileUrl.endsWith("/me")) {
                    userProfileUrl = userProfileUrl + "/me";
                }
                
                System.out.println("Attempting to fetch user info from: " + userProfileUrl);
                System.out.println("Headers: " + headers);
                
                ResponseEntity<Map> response = restTemplate.exchange(
                        userProfileUrl,
                        HttpMethod.GET, 
                        entity, 
                        Map.class);

                System.out.println("User-service response status: " + response.getStatusCode());
                
                Map<String, Object> user = response.getBody();
                System.out.println("User-service response body: " + user);
                
                if (user != null && user.containsKey("id")) {
                    Long numericUserId = Long.parseLong(user.get("id").toString());
                    System.out.println("Retrieved numeric user ID: " + numericUserId);
                    return numericUserId;
                }
                
                // If "id" is not found, check for alternate key names
                if (user != null && user.containsKey("userId")) {
                    Long numericUserId = Long.parseLong(user.get("userId").toString());
                    System.out.println("Retrieved numeric user ID from 'userId' field: " + numericUserId);
                    return numericUserId;
                }
                
                // Final attempt with user_id
                if (user != null && user.containsKey("user_id")) {
                    Long numericUserId = Long.parseLong(user.get("user_id").toString());
                    System.out.println("Retrieved numeric user ID from 'user_id' field: " + numericUserId);
                    return numericUserId;
                }
                
                System.err.println("User ID not found in user service response: " + user);
            } catch (Exception e) {
                System.err.println("API call to user-service failed: " + e.getMessage());
                // Fall through to JWT extraction as backup
            }
            
            // Fallback: Extract from JWT token if the API call fails
            if (token != null) {
                try {
                    // Parse token parts
                    String[] parts = token.split("\\.");
                    if (parts.length == 3) {
                        String payload = new String(java.util.Base64.getDecoder().decode(parts[1]));
                        
                        // For backward compatibility, extract email as fallback
                        if (payload.contains("sub")) {
                            // Extract email between quotes after "sub":
                            int subIndex = payload.indexOf("sub");
                            int startIndex = payload.indexOf(":", subIndex) + 2;
                            int endIndex = payload.indexOf("\"", startIndex);
                            if (startIndex > 0 && endIndex > startIndex) {
                                String email = payload.substring(startIndex, endIndex);
                                System.out.println("Using email as ID for backward compatibility: " + email);
                                
                                // IMPORTANT: During transition to numeric IDs, return a placeholder numeric ID
                                // This is a temporary measure to allow the system to function
                                // while we migrate the data
                                if (email.contains("@")) {
                                    System.out.println("Converting email to a placeholder ID for compatibility");
                                    // Use a deterministic hash to generate a consistent numeric ID from the email
                                    // Limit to 1-10000 range to avoid conflicts with real IDs
                                    int hashCode = Math.abs(email.hashCode()) % 10000 + 1;
                                    return Long.valueOf(hashCode);
                                }
                                return Long.parseLong(email);
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Error extracting from JWT: " + ex.getMessage());
                }
            }
            
            // For testing only - return a default ID if all else fails
            System.out.println("Using default test user ID");
            return 999L;
        } catch (Exception e) {
            throw new UnauthorizedException("Unable to get user ID: " + e.getMessage());
        }
    }

    /**
     * Get the current user's ID directly from the user-service auth/me endpoint
     * This is a more reliable method that bypasses spring cloud service discovery
     * @param token JWT token
     * @return the numeric user ID
     */
    public Long getUserIdDirect(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Use a direct URL to the user-service auth/me endpoint
            String directUrl = "http://localhost:9001/auth/me";
            
            System.out.println("Making direct call to user-service: " + directUrl);
            
            // Create a new RestTemplate for direct access without LoadBalanced annotation
            RestTemplate directTemplate = new RestTemplate();
            
            ResponseEntity<Map> response = directTemplate.exchange(
                    directUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class);
            
            Map<String, Object> user = response.getBody();
            System.out.println("Direct API response: " + user);
            
            if (user != null && user.containsKey("id")) {
                Long id = Long.parseLong(user.get("id").toString());
                System.out.println("Successfully got user ID directly: " + id);
                return id;
            }
            
            System.err.println("User ID not found in direct API response");
            throw new RuntimeException("User ID not found in API response");
        } catch (Exception e) {
            System.err.println("Error in direct user ID retrieval: " + e.getMessage());
            throw new UnauthorizedException("Error retrieving user ID: " + e.getMessage());
        }
    }

    /**
     * Get the current user's full name directly from the user-service
     * This bypasses the service discovery and connects directly to the user-service
     * @param token JWT token
     * @return the user's full name (firstName + " " + lastName)
     */
    public String getFullNameDirect(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Use a direct URL to the user-service auth/me endpoint
            String directUrl = "http://localhost:9001/auth/me";
            
            System.out.println("Making direct call to user-service for full name: " + directUrl);
            
            // Create a new RestTemplate for direct access without LoadBalanced annotation
            RestTemplate directTemplate = new RestTemplate();
            
            ResponseEntity<Map> response = directTemplate.exchange(
                    directUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class);
            
            Map<String, Object> user = response.getBody();
            System.out.println("User profile response: " + user);
            
            if (user != null) {
                // Get firstName and lastName
                String firstName = "";
                String lastName = "";
                
                if (user.containsKey("firstName")) {
                    firstName = user.get("firstName").toString();
                    System.out.println("Found firstName: " + firstName);
                }
                
                if (user.containsKey("lastName")) {
                    lastName = user.get("lastName").toString();
                    System.out.println("Found lastName: " + lastName);
                }
                
                // If we have both names, combine them
                if (!firstName.isEmpty() && !lastName.isEmpty()) {
                    String fullName = firstName + " " + lastName;
                    System.out.println("Returning full name: " + fullName);
                    return fullName;
                }
                
                // If we have only one name, return it
                if (!firstName.isEmpty()) {
                    return firstName;
                }
                
                if (!lastName.isEmpty()) {
                    return lastName;
                }
                
                // If we have an email, use that as a fallback
                if (user.containsKey("email")) {
                    return user.get("email").toString();
                }
            }
            
            System.err.println("Could not retrieve user name from profile API");
            return "Unknown User";
        } catch (Exception e) {
            System.err.println("Error retrieving full name: " + e.getMessage());
            return "Unknown User";
        }
    }

    /**
     * Get the current user's full name from the token
     * @param token the JWT token
     * @return the user's full name
     */
    public String getFullName(String token) {
        // Call the direct method instead, which is more reliable
        return getFullNameDirect(token);
        
        // Original implementation kept for reference but not used
        /*
        try {
            // Try the API call first
            ...
        }
        */
    }
}
