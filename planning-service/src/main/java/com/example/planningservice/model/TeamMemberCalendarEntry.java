package com.example.planningservice.model;

import java.time.LocalDate;

public class TeamMemberCalendarEntry {
    private Long userId;
    private String employeeName;
    private String teamName;
    private LocalDate date;
    private WorkStatus status;
    private Long requestId; // If there's a teletravail request associated

    // Enum for work status
    public enum WorkStatus {
        OFFICE,        // Default - assumed in office if no request
        TELETRAVAIL,   // Confirmed teletravail request
        PENDING,       // Pending teletravail request
        VACATION,      // On vacation
        MEETING        // In a meeting
    }

    // Constructors
    public TeamMemberCalendarEntry() {
    }

    public TeamMemberCalendarEntry(Long userId, String employeeName, String teamName, 
                                  LocalDate date, WorkStatus status, Long requestId) {
        this.userId = userId;
        this.employeeName = employeeName;
        this.teamName = teamName;
        this.date = date;
        this.status = status;
        this.requestId = requestId;
    }

    // Getters and Setters
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

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public WorkStatus getStatus() {
        return status;
    }

    public void setStatus(WorkStatus status) {
        this.status = status;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
}
