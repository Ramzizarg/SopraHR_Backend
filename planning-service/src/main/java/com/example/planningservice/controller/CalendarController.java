package com.example.planningservice.controller;

import com.example.planningservice.client.TeletravailClient;
import com.example.planningservice.model.TeamCalendarResponse;
import com.example.planningservice.service.CalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
public class CalendarController {

    private static final Logger logger = LoggerFactory.getLogger(CalendarController.class);
    private final CalendarService calendarService;

    @Autowired
    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /**
     * Get team calendar for a specific team and date range
     * 
     * @param teamName   Team name (e.g., "DEV", "QA")
     * @param startDate  Optional start date, defaults to current week Monday
     * @param endDate    Optional end date, defaults to current week Friday
     * @return Team calendar response
     */
    @GetMapping("/team/{teamName}")
    public ResponseEntity<TeamCalendarResponse> getTeamCalendar(
            @PathVariable String teamName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            // Default to current week (Monday to Friday) if dates not provided
            LocalDate today = LocalDate.now();
            LocalDate start = startDate != null ? startDate : today.with(java.time.DayOfWeek.MONDAY);
            LocalDate end = endDate != null ? endDate : today.with(java.time.DayOfWeek.FRIDAY);
            
            logger.info("Received calendar request for team {} from {} to {}", teamName, start, end);
            
            TeamCalendarResponse calendar = calendarService.getTeamCalendar(teamName, start, end);
            return ResponseEntity.ok(calendar);
        } catch (Exception e) {
            logger.error("Error processing calendar request for team {}: {}", teamName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Update the status of a teletravail request
     * Only team leaders and managers can update request status
     * 
     * @param requestId       ID of the request to update
     * @param statusUpdate    Status update request
     * @return Updated request
     */
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'MANAGER', 'ADMIN')")
    @PutMapping("/request/{requestId}/status")
    public ResponseEntity<?> updateRequestStatus(
            @PathVariable Long requestId,
            @RequestBody StatusUpdateDTO statusUpdate) {
        
        try {
            logger.info("Received request to update teletravail request {} to status {}", 
                    requestId, statusUpdate.getStatus());
            
            TeletravailClient.TeletravailResponse updated = calendarService.updateRequestStatus(
                    requestId, 
                    statusUpdate.getStatus(), 
                    statusUpdate.getRejectionReason()
            );
            
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.error("Error updating teletravail request {}: {}", requestId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update request: " + e.getMessage()));
        }
    }
    
    /**
     * DTO for status update requests
     */
    public static class StatusUpdateDTO {
        private String status;
        private String rejectionReason;
        
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
}
