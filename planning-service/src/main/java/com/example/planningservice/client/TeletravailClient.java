package com.example.planningservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "teletravail-service", url = "${services.teletravail-service.url}")
public interface TeletravailClient {
    
    /**
     * Get all teletravail requests within a date range
     * Uses the actual endpoint in TeletravailController
     */
    @GetMapping("/api/teletravail/requests/date-range")
    List<TeletravailResponse> getAllRequestsByDateRange(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate);
    
    /**
     * Get teletravail requests for a specific team within a date range
     */
    @GetMapping("/api/teletravail/team/{teamName}/date-range")
    List<TeletravailResponse> getTeamRequestsByDateRange(
            @PathVariable("teamName") String teamName,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate);
    
    /**
     * Get teletravail requests for a specific user within a date range
     */
    @GetMapping("/api/teletravail/user/{userId}/date-range")
    List<TeletravailResponse> getUserRequestsByDateRange(
            @PathVariable("userId") Long userId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate);
    
    /**
     * Update the status of a teletravail request
     */
    @PutMapping("/api/teletravail/requests/{requestId}")
    TeletravailResponse updateRequestStatus(
            @PathVariable("requestId") Long requestId,
            @RequestBody TeletravailRequestDTO statusUpdate);
    
    // DTO for teletravail request status updates (matching actual DTO structure)
    public static class TeletravailRequestDTO {
        private String status;
        private String rejectionReason;
        
        public TeletravailRequestDTO() {
        }
        
        public TeletravailRequestDTO(String status, String rejectionReason) {
            this.status = status;
            this.rejectionReason = rejectionReason;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getRejectionReason() {
            return rejectionReason;
        }
        
        public void setRejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
        }
    }
    
    // DTO for teletravail response (matching actual response structure)
    public static class TeletravailResponse {
        private Long id;
        private Long userId;
        private String userName;
        private String employeeName;
        private String team;
        private LocalDate teletravailDate;
        private String status;
        private String rejectionReason;
        private String createdAt;
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public String getUserName() {
            return userName;
        }
        
        public void setUserName(String userName) {
            this.userName = userName;
        }
        
        public String getEmployeeName() {
            return employeeName;
        }
        
        public void setEmployeeName(String employeeName) {
            this.employeeName = employeeName;
        }
        
        public String getTeam() {
            return team;
        }
        
        public void setTeam(String team) {
            this.team = team;
        }
        
        public LocalDate getTeletravailDate() {
            return teletravailDate;
        }
        
        public void setTeletravailDate(LocalDate teletravailDate) {
            this.teletravailDate = teletravailDate;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getRejectionReason() {
            return rejectionReason;
        }
        
        public void setRejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
        }
        
        public String getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}
