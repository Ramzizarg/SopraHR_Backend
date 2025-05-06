package com.example.planningservice.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class TokenDebugController {
    private static final Logger logger = LoggerFactory.getLogger(TokenDebugController.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> debugToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("status", "error");
            response.put("message", "No Bearer token provided");
            return ResponseEntity.ok(response);
        }
        
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecret.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            response.put("status", "success");
            response.put("subject", claims.getSubject());
            response.put("claims", claims);
            
            logger.info("Token debug - Subject: {}, Claims: {}", claims.getSubject(), claims);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error parsing token: " + e.getMessage());
            logger.error("Error parsing token", e);
            return ResponseEntity.ok(response);
        }
    }
}
