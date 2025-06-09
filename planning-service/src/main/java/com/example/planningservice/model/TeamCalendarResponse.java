package com.example.planningservice.model;

import java.time.LocalDate;
import java.util.List;

public class TeamCalendarResponse {
    private String teamName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<TeamMember> teamMembers;

    public static class TeamMember {
        private Long userId;
        private String employeeName;
        private String role; // User role (EMPLOYEE, TEAM_LEADER, MANAGER, ADMIN)
        private List<DailyStatus> dailyStatuses;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        public void setEmployeeName(String employeeName) {
            this.employeeName = employeeName;
        }
        
        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<DailyStatus> getDailyStatuses() {
            return dailyStatuses;
        }

        public void setDailyStatuses(List<DailyStatus> dailyStatuses) {
            this.dailyStatuses = dailyStatuses;
        }
    }

    public static class DailyStatus {
        private LocalDate date;
        private TeamMemberCalendarEntry.WorkStatus status;
        private Long requestId;

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public TeamMemberCalendarEntry.WorkStatus getStatus() {
            return status;
        }

        public void setStatus(TeamMemberCalendarEntry.WorkStatus status) {
            this.status = status;
        }

        public Long getRequestId() {
            return requestId;
        }

        public void setRequestId(Long requestId) {
            this.requestId = requestId;
        }
    }

    // Getters and Setters
    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<TeamMember> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(List<TeamMember> teamMembers) {
        this.teamMembers = teamMembers;
    }
}
