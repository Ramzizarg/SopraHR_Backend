package com.example.planningservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@FeignClient(name = "user-service", url = "http://localhost:9001", fallback = UserClient.UserClientFallback.class)
public interface UserClient {
    
    @GetMapping("/api/users/{userId}")
    UserDTO getUserById(
        @PathVariable("userId") Long userId,
        @RequestHeader("Authorization") String authHeader);
    
    @GetMapping("/api/users/name/{userId}")
    ResponseEntity<String> getUserFullName(
        @PathVariable("userId") Long userId,
        @RequestHeader("Authorization") String authHeader);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class UserDTO {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String role;
    }
    
    @Component
    @Slf4j
    class UserClientFallback implements UserClient {
        @Override
        public UserDTO getUserById(Long userId, String authHeader) {
            log.error("Circuit breaker triggered: Could not retrieve user details for ID {}", userId);
            UserDTO fallbackUser = new UserDTO();
            fallbackUser.setId(userId);
            fallbackUser.setFirstName("User");
            fallbackUser.setLastName(userId.toString());
            return fallbackUser;
        }
        
        @Override
        public ResponseEntity<String> getUserFullName(Long userId, String authHeader) {
            log.error("Circuit breaker triggered: Could not retrieve user name for ID {}", userId);
            return ResponseEntity.ok("User " + userId);
        }
    }
}
