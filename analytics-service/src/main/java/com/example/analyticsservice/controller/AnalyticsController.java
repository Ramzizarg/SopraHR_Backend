package com.example.analyticsservice.controller;

import com.example.analyticsservice.dto.AnalyticsResponse;
import com.example.analyticsservice.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for analytics endpoints
 * Exposes aggregated data from user-service, teletravail-service, reservation-service, and planning-service
 * Updated to work with new analytics endpoints from all microservices
 */
@RestController
@RequestMapping("/api/v1/analytics")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.OPTIONS})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Get comprehensive dashboard metrics aggregating data from all microservices
     * @param authHeader HTTP request with Authorization header containing JWT token
     * @return Dashboard metrics with data from user-service, teletravail-service, reservation-service, and planning-service
     */
    @GetMapping("/dashboard")
    public Mono<ResponseEntity<Map<String, Object>>> getDashboardMetrics(
            @RequestHeader("Authorization") String authHeader) {
        // Extract token from Authorization header
        String token = extractToken(authHeader);
        
        System.out.println("Dashboard metrics requested with token: " + (token != null && !token.isEmpty() ? "[VALID]" : "[MISSING]"));
        
        if (token == null || token.isEmpty()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null));
        }
        
        return analyticsService.getAnalytics(token)
                .map(response -> {
                    // Log successful analytics request
                    Map<String, Object> data = response.getData();
                    Map<String, Object> dashboardSummary = (Map<String, Object>) data.getOrDefault("dashboardSummary", new HashMap<>());
                    
                    System.out.println("Successfully retrieved analytics data from all services");
                    return ResponseEntity.ok(data);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(e -> {
                    System.err.println("Error processing analytics data: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Get weekly analytics data from all services
     * @param authHeader Authorization header containing JWT token
     * @return Weekly analytics data from all services
     */
    @GetMapping("/weekly")
    public Mono<ResponseEntity<Map<String, Object>>> getWeeklyAnalytics(
            @RequestHeader("Authorization") String authHeader) {
        
        // Extract token from Authorization header
        String token = extractToken(authHeader);
        
        if (token == null || token.isEmpty()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null));
        }
        
        // Get current week's dates
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(DayOfWeek.MONDAY);
        LocalDate friday = now.with(DayOfWeek.FRIDAY);
        
        // Format dates for display
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        return analyticsService.getAnalytics(token)
                .map(response -> {
                    Map<String, Object> data = response.getData();
                    
                    // Extract weekly data
                    Map<String, Object> result = new HashMap<>();
                    result.put("startDate", monday.format(formatter));
                    result.put("endDate", friday.format(formatter));
                    
                    // Get reservation weekly distribution
                    Map<String, Object> reservationAnalytics = 
                            (Map<String, Object>) data.getOrDefault("reservationAnalytics", new HashMap<>());
                    if (reservationAnalytics.containsKey("dailyDistribution")) {
                        result.put("dailyDistribution", reservationAnalytics.get("dailyDistribution"));
                    }
                    
                    // Get telework weekly distribution
                    Map<String, Object> teleworkAnalytics = 
                            (Map<String, Object>) data.getOrDefault("teleworkAnalytics", new HashMap<>());
                    if (teleworkAnalytics.containsKey("currentWeekRequests")) {
                        result.put("teleworkRequests", teleworkAnalytics.get("currentWeekRequests"));
                    }
                    
                    // Get planning weekly distribution
                    Map<String, Object> planningAnalytics = 
                            (Map<String, Object>) data.getOrDefault("planningAnalytics", new HashMap<>());
                    if (planningAnalytics.containsKey("currentWeekEntries")) {
                        result.put("planningEntries", planningAnalytics.get("currentWeekEntries"));
                    }
                    if (planningAnalytics.containsKey("currentWeekByTeam")) {
                        result.put("planningTeamDistribution", planningAnalytics.get("currentWeekByTeam"));
                    }
                    
                    return ResponseEntity.ok(result);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(e -> {
                    System.err.println("Error processing weekly analytics: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Get team-specific analytics data from all services
     * @param team Team name to filter by (DEV, QA, HR, etc.)
     * @param authHeader Authorization header containing JWT token
     * @return Team analytics data with member details, reservations, telework status, and planning
     */
    @GetMapping("/team/{team}")
    public Mono<ResponseEntity<Map<String, Object>>> getTeamAnalytics(
            @PathVariable String team,
            @RequestHeader("Authorization") String authHeader) {
        
        // Extract token from Authorization header
        String token = extractToken(authHeader);
        
        if (token == null || token.isEmpty()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null));
        }
        
        return analyticsService.getAnalytics(token)
                .map(response -> {
                    Map<String, Object> data = response.getData();
                    Map<String, Object> result = new HashMap<>();
                    result.put("team", team);
                    
                    // Get user team distribution
                    Map<String, Object> userAnalytics = 
                            (Map<String, Object>) data.getOrDefault("userAnalytics", new HashMap<>());
                    if (userAnalytics.containsKey("teamDistribution")) {
                        List<Map<String, Object>> teamDistribution = 
                                (List<Map<String, Object>>) userAnalytics.get("teamDistribution");
                        
                        // Filter for the requested team
                        teamDistribution.stream()
                                .filter(t -> team.equalsIgnoreCase((String) t.get("team")))
                                .findFirst()
                                .ifPresent(t -> result.put("teamData", t));
                    }
                    
                    // Get all users and filter by team
                    List<Object> allUsers = (List<Object>) data.getOrDefault("users", new ArrayList<>());
                    List<Object> teamMembers = allUsers.stream()
                            .filter(u -> {
                                if (u instanceof Map) {
                                    Map<String, Object> user = (Map<String, Object>) u;
                                    return team.equalsIgnoreCase((String) user.get("team"));
                                }
                                return false;
                            })
                            .collect(Collectors.toList());
                    result.put("teamMembers", teamMembers);
                    
                    // Filter telework requests by team
                    Map<String, Object> teleworkAnalytics = 
                            (Map<String, Object>) data.getOrDefault("teleworkAnalytics", new HashMap<>());
                    if (teleworkAnalytics.containsKey("teamDistribution")) {
                        List<Map<String, Object>> teamDistribution = 
                                (List<Map<String, Object>>) teleworkAnalytics.get("teamDistribution");
                        
                        teamDistribution.stream()
                                .filter(t -> team.equalsIgnoreCase((String) t.get("team")))
                                .findFirst()
                                .ifPresent(t -> result.put("teleworkData", t));
                    }
                    
                    // Get planning data for team
                    Map<String, Object> planningAnalytics = 
                            (Map<String, Object>) data.getOrDefault("planningAnalytics", new HashMap<>());
                    if (planningAnalytics.containsKey("currentWeekByTeam")) {
                        Map<String, Object> teamEntries = 
                                (Map<String, Object>) planningAnalytics.get("currentWeekByTeam");
                        
                        if (teamEntries.containsKey(team)) {
                            result.put("planningData", teamEntries.get(team));
                        }
                    }
                    
                    return ResponseEntity.ok(result);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(e -> {
                    System.err.println("Error processing team analytics for '" + team + "': " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Extract Bearer token from Authorization header
     * @param authHeader Authorization header value
     * @return The extracted token or null if not found
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}