package com.example.planningservice.service;

import com.example.planningservice.client.TeletravailClient;
import com.example.planningservice.client.UserClient;
import com.example.planningservice.model.TeamCalendarResponse;
import com.example.planningservice.model.TeamMemberCalendarEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    private static final Logger logger = LoggerFactory.getLogger(CalendarService.class);
    private final UserClient userClient;
    private final TeletravailClient teletravailClient;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public CalendarService(UserClient userClient, TeletravailClient teletravailClient) {
        this.userClient = userClient;
        this.teletravailClient = teletravailClient;
    }

    /**
     * Get the team calendar for a specific team within a date range
     * 
     * @param teamName   Team name (e.g., "DEV", "QA")
     * @param startDate  Start date of the period
     * @param endDate    End date of the period
     * @return Team calendar response with status for each team member
     */
    public TeamCalendarResponse getTeamCalendar(String teamName, LocalDate startDate, LocalDate endDate) {
        logger.info("Generating team calendar for team {} from {} to {}", teamName, startDate, endDate);
        
        if (teamName == null || teamName.isEmpty() || "DEFAULT".equals(teamName)) {
            // Use a default team if not specified
            teamName = "DEV";
            logger.info("No team name provided, defaulting to {}", teamName);
        }
        
        final String finalTeamName = teamName; // Create final copy for lambda
        
        // 1. Get all team members from User Service
        List<UserClient.UserResponse> teamMembers;
        try {
            teamMembers = userClient.getUsersByTeam(finalTeamName);
            logger.info("Retrieved {} team members for team {}", teamMembers.size(), finalTeamName);
        } catch (Exception e) {
            logger.error("Error fetching team members for team {}: {}", finalTeamName, e.getMessage());
            teamMembers = new ArrayList<>(); // Empty list if there's an error
        }
        
        // 2. Get all teletravail requests for this team and date range
        List<TeletravailClient.TeletravailResponse> teletravailRequests;
        try {
            teletravailRequests = teletravailClient.getTeamRequestsByDateRange(
                teamName, 
                startDate.format(DATE_FORMATTER), 
                endDate.format(DATE_FORMATTER)
            );
            logger.info("Retrieved {} teletravail requests for team {} in the date range", 
                    teletravailRequests.size(), teamName);
        } catch (Exception e) {
            logger.error("Error fetching teletravail requests for team {}: {}", teamName, e.getMessage());
            teletravailRequests = new ArrayList<>(); // Empty list if there's an error
        }
        
        // 3. Create the calendar response
        TeamCalendarResponse response = new TeamCalendarResponse();
        response.setTeamName(teamName);
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        
        // Create a final reference to teletravailRequests for use in the lambda expression
        final List<TeletravailClient.TeletravailResponse> finalTeletravailRequests = teletravailRequests;
        
        // 4. Process team members and their statuses
        List<TeamCalendarResponse.TeamMember> processedMembers = teamMembers.stream()
            .map(member -> createTeamMemberWithStatuses(member, startDate, endDate, finalTeletravailRequests))
            .collect(Collectors.toList());
        
        // Ensure the calendar isn't empty - log a warning if no team members found
        if (processedMembers.isEmpty()) {
            logger.warn("No team members found for team {}. Calendar will be empty.", finalTeamName);
        }
        
        response.setTeamMembers(processedMembers);
        logger.info("Successfully generated calendar with {} team members", processedMembers.size());
        return response;
    }
    
    /**
     * Create a team member with status for each day in the date range
     */
    private TeamCalendarResponse.TeamMember createTeamMemberWithStatuses(
            UserClient.UserResponse member, 
            LocalDate startDate, 
            LocalDate endDate,
            List<TeletravailClient.TeletravailResponse> teletravailRequests) {
        
        TeamCalendarResponse.TeamMember teamMember = new TeamCalendarResponse.TeamMember();
        teamMember.setUserId(member.getId());
        
        // Get employee name - prioritizing the employeeName field in UserResponse (per system memory)
        // The TeletravailRequest entity now contains an employeeName field for proper employee name display
        String employeeName = null;
        
        // Check for employeeName field
        if (member.getEmployeeName() != null && !member.getEmployeeName().isEmpty()) {
            employeeName = member.getEmployeeName();
        } 
        // Fall back to combining firstName and lastName if available
        else if (member.getFirstName() != null && member.getLastName() != null) {
            employeeName = member.getFirstName() + " " + member.getLastName();
        }
        // Last resort fallback
        else {
            employeeName = "User " + member.getId();
        }
        
        teamMember.setEmployeeName(employeeName);
        
        // Set user's role
        teamMember.setRole(member.getRole());
        
        logger.debug("Processing calendar for team member: {} (ID: {})", employeeName, member.getId());
        
        // Create final reference for use in lambda to avoid "variable used in lambda should be final" error
        final Long userId = member.getId();
        
        // Filter requests for this user
        List<TeletravailClient.TeletravailResponse> userRequests = teletravailRequests.stream()
            .filter(req -> req.getUserId() != null && req.getUserId().equals(userId))
            .collect(Collectors.toList());
        
        logger.debug("Found {} teletravail requests for user {}", userRequests.size(), userId);
        
        // Map of date to teletravail request for this user
        Map<LocalDate, TeletravailClient.TeletravailResponse> requestsByDate = userRequests.stream()
            .collect(Collectors.toMap(
                TeletravailClient.TeletravailResponse::getTeletravailDate, 
                Function.identity(), // Use method reference instead of lambda
                (existing, replacement) -> existing // In case of duplicates, keep the first
            ));
        
        // Create daily statuses for the date range
        List<TeamCalendarResponse.DailyStatus> dailyStatuses = new ArrayList<>();
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            TeamCalendarResponse.DailyStatus dailyStatus = new TeamCalendarResponse.DailyStatus();
            dailyStatus.setDate(current);
            
            // Check if there's a teletravail request for this date
            if (requestsByDate.containsKey(current)) {
                TeletravailClient.TeletravailResponse request = requestsByDate.get(current);
                dailyStatus.setRequestId(request.getId());
                
                // Map status based on request status
                switch (request.getStatus().toUpperCase()) {
                    case "APPROVED":
                    case "CONFIRMED":
                        // APPROVED = Télétravail
                        dailyStatus.setStatus(TeamMemberCalendarEntry.WorkStatus.TELETRAVAIL);
                        break;
                    case "PENDING":
                        // PENDING = En attente
                        dailyStatus.setStatus(TeamMemberCalendarEntry.WorkStatus.PENDING);
                        break;
                    case "REJECTED":
                    case "REFUSED":
                    default:
                        // All others = OFFICE
                        dailyStatus.setStatus(TeamMemberCalendarEntry.WorkStatus.OFFICE);
                }
            } else {
                // No request, assume OFFICE as the default
                dailyStatus.setStatus(TeamMemberCalendarEntry.WorkStatus.OFFICE);
            }
            
            dailyStatuses.add(dailyStatus);
            current = current.plusDays(1);
        }
        
        teamMember.setDailyStatuses(dailyStatuses);
        return teamMember;
    }
    
    /**
     * Update a teletravail request status
     * 
     * @param requestId       The ID of the request to update
     * @param status          New status (APPROVED or REJECTED)
     * @param rejectionReason Reason for rejection (required if status is REJECTED)
     * @return Updated teletravail request
     */
    public TeletravailClient.TeletravailResponse updateRequestStatus(
            Long requestId, 
            String status, 
            String rejectionReason) {
        logger.info("Updating teletravail request {} to status {}", requestId, status);
        
        TeletravailClient.TeletravailRequestDTO updateRequest = new TeletravailClient.TeletravailRequestDTO();
        updateRequest.setStatus(status);
        if ("REJECTED".equals(status.toUpperCase()) || "REFUSED".equals(status.toUpperCase())) {
            updateRequest.setRejectionReason(rejectionReason);
            logger.info("Including rejection reason for request {}", requestId);
        }
        
        try {
            TeletravailClient.TeletravailResponse response = teletravailClient.updateRequestStatus(requestId, updateRequest);
            logger.info("Successfully updated teletravail request {}", requestId);
            return response;
        } catch (Exception e) {
            logger.error("Error updating teletravail request {}: {}", requestId, e.getMessage());
            throw e; // Re-throw to let the controller handle it
        }
    }
}
