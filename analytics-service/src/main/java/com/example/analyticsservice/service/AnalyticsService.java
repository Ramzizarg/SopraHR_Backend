package com.example.analyticsservice.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.analyticsservice.dto.AnalyticsResponse;
import com.example.analyticsservice.model.AnalyticsMetrics;

import reactor.core.publisher.Mono;

/**
 * Service layer for analytics data processing
 * Consumes data from all other microservices: user-service, teletravail-service, reservation-service, planning-service
 */
@Service
public class AnalyticsService {

    private final WebClient.Builder webClientBuilder;
    
    @Value("${services.user-service.url}")
    private String userServiceUrl;
    
    @Value("${services.teletravail-service.url}")
    private String teletravailServiceUrl;
    
    @Value("${services.reservation-service.url}")
    private String reservationsServiceUrl;
    
    @Value("${services.planning-service.url}")
    private String planningServiceUrl;

    public AnalyticsService(WebClient.Builder webClientBuilder) {
        super();
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<AnalyticsResponse> getAnalytics(String token) {
        // Fetch analytics data from each service's dedicated analytics endpoints
        Mono<Map> userAnalyticsFuture = fetchUserAnalytics(token);
        Mono<Map> reservationAnalyticsFuture = fetchReservationAnalytics(token);
        Mono<Map> teleworkAnalyticsFuture = fetchTeleworkAnalytics(token);
        Mono<Map> planningAnalyticsFuture = fetchPlanningAnalytics(token);
        
        // Also fetch some additional data that might be needed
        Mono<List> usersFuture = fetchUsers(token);
        Mono<List> currentWeekReservationsFuture = fetchCurrentWeekReservations(token);
        Mono<List> upcomingTeleworkFuture = fetchUpcomingTeleworkRequests(token);

        // Wait for all futures to complete and combine the results
        return Mono.zip(
                userAnalyticsFuture,
                reservationAnalyticsFuture, 
                teleworkAnalyticsFuture,
                planningAnalyticsFuture,
                usersFuture,
                currentWeekReservationsFuture,
                upcomingTeleworkFuture
            ).map(tuple -> {
                Map<String, Object> userAnalytics = tuple.getT1();
                Map<String, Object> reservationAnalytics = tuple.getT2();
                Map<String, Object> teleworkAnalytics = tuple.getT3();
                Map<String, Object> planningAnalytics = tuple.getT4();
                List<Object> users = tuple.getT5();
                List<Object> currentWeekReservations = tuple.getT6();
                List<Object> upcomingTelework = tuple.getT7();

                // Process the data to create the AnalyticsResponse
                Map<String, Object> analyticsData = new HashMap<>();
                
                // Add the processed analytics data
                analyticsData.put("userAnalytics", userAnalytics);
                analyticsData.put("reservationAnalytics", reservationAnalytics);
                analyticsData.put("teleworkAnalytics", teleworkAnalytics);
                analyticsData.put("planningAnalytics", planningAnalytics);
                
                // Add some raw data for specific UI components
                analyticsData.put("users", users);
                analyticsData.put("currentWeekReservations", currentWeekReservations);
                analyticsData.put("upcomingTelework", upcomingTelework);
                
                // Create dashboard summary
                Map<String, Object> dashboardSummary = new HashMap<>();
                
                // User metrics
                dashboardSummary.put("totalUsers", userAnalytics.getOrDefault("totalUsers", 0));
                
                // Reservation metrics
                dashboardSummary.put("totalReservations", reservationAnalytics.getOrDefault("totalReservations", 0));
                dashboardSummary.put("todayReservations", reservationAnalytics.getOrDefault("todayReservations", 0));
                dashboardSummary.put("workstationUtilizationRate", reservationAnalytics.getOrDefault("workstationUtilizationRate", 0));
                
                // Telework metrics
                dashboardSummary.put("totalTeleworkRequests", teleworkAnalytics.getOrDefault("totalRequests", 0));
                dashboardSummary.put("pendingTeleworkRequests", teleworkAnalytics.getOrDefault("pendingRequests", 0));
                dashboardSummary.put("teleworkApprovalRate", teleworkAnalytics.getOrDefault("approvalRate", 0));
                
                // Planning metrics
                dashboardSummary.put("todayPlanningEntries", planningAnalytics.getOrDefault("todayEntries", 0));
                dashboardSummary.put("upcomingPlanningEntries", planningAnalytics.getOrDefault("upcomingEntries", 0));
                
                analyticsData.put("dashboardSummary", dashboardSummary);
                
                // Return the final response
                return new AnalyticsResponse(analyticsData);
            });
    }
    
    /**
     * Fetch analytics data from the user service
     * @param token Authentication token
     * @return Map containing user analytics data
     */
    private Mono<Map> fetchUserAnalytics(String token) {
        String url = userServiceUrl + "/api/users/analytics/dashboard";
        System.out.println("Fetching user analytics from: " + url);
        return webClientBuilder.build()
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(data -> System.out.println("Successfully retrieved user analytics data"))
                .onErrorResume(error -> {
                    System.err.println("Error fetching user analytics from " + url + ": " + error.getMessage());
                    return Mono.just(new HashMap<>());
                });
    }
    
    /**
     * Fetch analytics data from the reservation service
     * @param token Authentication token
     * @return Map containing reservation analytics data
     */
    private Mono<Map> fetchReservationAnalytics(String token) {
        String url = reservationsServiceUrl + "/api/v1/reservations/analytics/dashboard";
        System.out.println("Fetching reservation analytics from: " + url);
        return webClientBuilder.build()
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(data -> System.out.println("Successfully retrieved reservation analytics data"))
                .onErrorResume(error -> {
                    System.err.println("Error fetching reservation analytics from " + url + ": " + error.getMessage());
                    return Mono.just(new HashMap<>());
                });
    }
    
    /**
     * Fetch analytics data from the telework service
     * @param token Authentication token
     * @return Map containing telework analytics data
     */
    private Mono<Map> fetchTeleworkAnalytics(String token) {
        String url = teletravailServiceUrl + "/api/teletravail/analytics/dashboard";
        System.out.println("Fetching telework analytics from: " + url);
        return webClientBuilder.build()
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(data -> System.out.println("Successfully retrieved telework analytics data"))
                .onErrorResume(error -> {
                    System.err.println("Error fetching telework analytics from " + url + ": " + error.getMessage());
                    return Mono.just(new HashMap<>());
                });
    }
    
    /**
     * Fetch analytics data from the planning service
     * @param token Authentication token
     * @return Map containing planning analytics data
     */
    private Mono<Map> fetchPlanningAnalytics(String token) {
        String url = planningServiceUrl + "/api/calendar/analytics/dashboard";
        System.out.println("Fetching planning analytics from: " + url);
        return webClientBuilder.build()
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(data -> System.out.println("Successfully retrieved planning analytics data"))
                .onErrorResume(error -> {
                    System.err.println("Error fetching planning analytics from " + url + ": " + error.getMessage());
                    return Mono.just(new HashMap<>());
                });
    }

    /**
     * Fetch all users from user-service
     * @param token Authentication token
     * @return List of user data
     */
    private Mono<List> fetchUsers(String token) {
        System.out.println("Fetching users from: " + userServiceUrl + "/api/users");
        return webClientBuilder.build()
                .get()
                .uri(userServiceUrl + "/api/users")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(List.class)
                .doOnSuccess(data -> System.out.println("Successfully retrieved " + (data != null ? ((List)data).size() : 0) + " users"))
                .onErrorResume(error -> {
                    System.err.println("Error fetching users from " + userServiceUrl + ": " + error.getMessage());
                    // Return empty list on error rather than failing completely
                    return Mono.just(new ArrayList<>());
                });
    }
    
    /**
     * Fetch current week reservations from reservation-service
     * @param token Authentication token
     * @return List of reservation data for the current week
     */
    private Mono<List> fetchCurrentWeekReservations(String token) {
        // Get current week's Monday and Friday
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        
        String url = reservationsServiceUrl + "/api/v1/reservations/daterange?startDate=" 
                + monday.toString() + "&endDate=" + friday.toString();
        
        System.out.println("Fetching current week reservations from: " + url);
        return webClientBuilder.build()
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(List.class)
                .doOnSuccess(data -> System.out.println("Successfully retrieved " + 
                        (data != null ? ((List)data).size() : 0) + " reservations for current week"))
                .onErrorResume(error -> {
                    System.err.println("Error fetching current week reservations from " + 
                            url + ": " + error.getMessage());
                    return Mono.just(new ArrayList<>());
                });
    }
    
    /**
     * Fetch upcoming telework requests from teletravail-service
     * @param token Authentication token
     * @return List of upcoming telework requests
     */
    private Mono<List> fetchUpcomingTeleworkRequests(String token) {
        String url = teletravailServiceUrl + "/api/teletravail/upcoming";
        System.out.println("Fetching upcoming telework requests from: " + url);
        
        return webClientBuilder.build()
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(List.class)
                .doOnSuccess(data -> System.out.println("Successfully retrieved " + 
                        (data != null ? ((List)data).size() : 0) + " upcoming telework requests"))
                .onErrorResume(error -> {
                    System.err.println("Error fetching upcoming telework requests from " + 
                            url + ": " + error.getMessage());
                    return Mono.just(new ArrayList<>());
                });
    }
    
    /**
     * Fetch planning data from planning-service
     * Uses the team calendar endpoint to get all teams' planning data
     * @param token Authentication token
     * @return List of planning entries from all teams
     */
    private Mono<List> fetchPlanningData(String token) {
        // Get current week's Monday and Friday
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        
        String planningUrl = planningServiceUrl + "/api/calendar/team/ALL" + 
                "?startDate=" + monday.toString() + "&endDate=" + friday.toString();
        System.out.println("Fetching planning data from: " + planningUrl);
        return webClientBuilder.build()
                .get()
                .uri(planningUrl)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class) // Change to Map instead of List
                .map(responseMap -> {
                    // Extract the entries from the response object
                    if (responseMap.containsKey("entries") && responseMap.get("entries") instanceof List) {
                        return (List) responseMap.get("entries");
                    } else {
                        // If entries key doesn't exist or isn't a list, return empty list
                        System.out.println("Planning data doesn't contain 'entries' list, got: " + responseMap.keySet());
                        return new ArrayList<>();
                    }
                })
                .doOnSuccess(data -> System.out.println("Successfully processed planning data for week " + monday + " to " + friday))
                .onErrorResume(error -> {
                    System.err.println("Error fetching planning data from " + planningServiceUrl + ": " + error.getMessage());
                    // Return empty list on error rather than failing completely
                    return Mono.just(new ArrayList<>());
                });
    }

    public Mono<AnalyticsMetrics> getDashboardMetrics(String token) {
        Mono<Map> userAnalyticsFuture = fetchUserAnalytics(token);
        Mono<Map> reservationAnalyticsFuture = fetchReservationAnalytics(token);
        Mono<Map> teleworkAnalyticsFuture = fetchTeleworkAnalytics(token);
        Mono<Map> planningAnalyticsFuture = fetchPlanningAnalytics(token);
        Mono<List> usersFuture = fetchUsers(token);

        return Mono.zip(
            userAnalyticsFuture,
            reservationAnalyticsFuture,
            teleworkAnalyticsFuture,
            planningAnalyticsFuture,
            usersFuture
        ).map(tuple -> {
            Map<String, Object> userAnalytics = tuple.getT1();
            Map<String, Object> reservationAnalytics = tuple.getT2();
            Map<String, Object> teleworkAnalytics = tuple.getT3();
            Map<String, Object> planningAnalytics = tuple.getT4();
            List<Map<String, Object>> users = (List<Map<String, Object>>) tuple.getT5();

            AnalyticsMetrics metrics = new AnalyticsMetrics();

            // All Employees from user-service
            List<AnalyticsMetrics.Employee> allEmployees = users.stream()
                .map(u -> new AnalyticsMetrics.Employee(
                    ((Number) u.get("id")).longValue(),
                    (String) u.get("firstName"),
                    (String) u.get("lastName"),
                    (String) u.get("team"),
                    (String) u.get("email")
                ))
                .collect(Collectors.toList());
            metrics.setAllEmployees(allEmployees);
            
            // Total Employees from user-service
            metrics.setTotalEmployees(allEmployees.size());

            // Simulate fetching previous month's employee count to calculate growth
            int previousEmployees = allEmployees.size() - 5; // Placeholder
            if (previousEmployees > 0) {
                double growth = ((double) (allEmployees.size() - previousEmployees) / previousEmployees) * 100;
                metrics.setEmployeeGrowthRate(growth);
            } else {
                metrics.setEmployeeGrowthRate(100.0); // Default growth if no previous data
            }

            // Office Presence and Remote Work based on telework requests
            int approvedTeleworkToday = ((Number) teleworkAnalytics.getOrDefault("approvedTodayCount", 0)).intValue();
            metrics.setRemoteWork(approvedTeleworkToday);

            int inOfficeToday = allEmployees.size() - approvedTeleworkToday;
            metrics.setOfficePresence(inOfficeToday);

            if (allEmployees.size() > 0) {
                double presencePercentage = ((double) inOfficeToday / allEmployees.size()) * 100;
                metrics.setOfficePresencePercentage(presencePercentage);
            } else {
                metrics.setOfficePresencePercentage(0.0);
            }

            // Simulate change from yesterday for the badge
            int yesterdayOfficePresence = inOfficeToday > 0 ? inOfficeToday - 2 : 0; // Placeholder
            if (yesterdayOfficePresence > 0) {
                double change = ((double) (inOfficeToday - yesterdayOfficePresence) / yesterdayOfficePresence) * 100;
                metrics.setOfficePresenceChange(change);
            } else if (inOfficeToday > 0) {
                metrics.setOfficePresenceChange(100.0);
            } else {
                metrics.setOfficePresenceChange(0.0);
            }

            // Reservation Count and Occupancy Rate from reservation-service
            metrics.setReservationCount(((Number) reservationAnalytics.getOrDefault("totalReservations", 0)).intValue());
            // Assuming occupancyRate is a percentage value (e.g., 80.5) and needs to be an int
            metrics.setOccupancyRate(((Number) reservationAnalytics.getOrDefault("occupancyRate", 0.0)).intValue());

            // Team Distribution from user-service
            List<Map<String, Object>> teamDistData = (List<Map<String, Object>>) userAnalytics.getOrDefault("teamDistribution", new ArrayList<>());
            List<AnalyticsMetrics.TeamDistribution> teamDist = teamDistData.stream()
                .map(d -> new AnalyticsMetrics.TeamDistribution((String) d.get("team"), ((Number) d.get("count")).intValue()))
                .collect(Collectors.toList());
            metrics.setTeamDistribution(teamDist);

            // Weekly Occupancy from reservation-service
            List<Map<String, Object>> weeklyOccData = (List<Map<String, Object>>) reservationAnalytics.getOrDefault("weeklyOccupancy", new ArrayList<>());
            List<AnalyticsMetrics.WeeklyOccupancy> weeklyOcc = weeklyOccData.stream()
                .map(d -> new AnalyticsMetrics.WeeklyOccupancy(
                    (String) d.get("day"),
                    ((Number) d.get("percentage")).intValue()))
                .collect(Collectors.toList());
            metrics.setWeeklyOccupancy(weeklyOcc);

            // Approval Rates from teletravail-service
            int approved = ((Number) teleworkAnalytics.getOrDefault("approvedRequests", 0)).intValue();
            int rejected = ((Number) teleworkAnalytics.getOrDefault("rejectedRequests", 0)).intValue();
            int pending = ((Number) teleworkAnalytics.getOrDefault("pendingRequests", 0)).intValue();
            metrics.setApprovalRates(new AnalyticsMetrics.ApprovalRates(approved, rejected, pending));

            return metrics;
        });
    }
}