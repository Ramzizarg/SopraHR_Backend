package com.example.analyticsservice.model;

import java.util.List;

/**
 * Model class representing comprehensive analytics metrics for the admin dashboard
 */
public class AnalyticsMetrics {
    private int totalEmployees;
    private int officePresence;
    private int remoteWork;
    private int reservationCount;
    private int occupancyRate;
    private double employeeGrowthRate;
    private double officePresencePercentage;
    private double officePresenceChange;
    private List<TeamDistribution> teamDistribution;
    private List<WeeklyOccupancy> weeklyOccupancy;
    private ApprovalRates approvalRates;
    private List<Employee> allEmployees;

    public AnalyticsMetrics() {
        super();
    }

    // Nested classes for specific metric categories
    public static class TeamDistribution {
        private String team;
        private int count;

        public TeamDistribution() {
            super();
        }

        public TeamDistribution(String team, int count) {
            super();
            this.team = team;
            this.count = count;
        }

        public String getTeam() {
            return team;
        }

        public void setTeam(String team) {
            this.team = team;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    public static class WeeklyOccupancy {
        private String day;
        private int percentage;

        public WeeklyOccupancy() {
            super();
        }

        public WeeklyOccupancy(String day, int percentage) {
            super();
            this.day = day;
            this.percentage = percentage;
        }

        public String getDay() {
            return day;
        }

        public void setDay(String day) {
            this.day = day;
        }

        public int getPercentage() {
            return percentage;
        }

        public void setPercentage(int percentage) {
            this.percentage = percentage;
        }
    }

    public static class ApprovalRates {
        private int approved;
        private int rejected;
        private int pending;

        public ApprovalRates() {
            super();
        }

        public ApprovalRates(int approved, int rejected, int pending) {
            super();
            this.approved = approved;
            this.rejected = rejected;
            this.pending = pending;
        }

        public int getApproved() {
            return approved;
        }

        public void setApproved(int approved) {
            this.approved = approved;
        }

        public int getRejected() {
            return rejected;
        }

        public void setRejected(int rejected) {
            this.rejected = rejected;
        }

        public int getPending() {
            return pending;
        }

        public void setPending(int pending) {
            this.pending = pending;
        }
    }

    public static class Employee {
        private Long id;
        private String firstName;
        private String lastName;
        private String team;
        private String email;

        public Employee() {
            super();
        }

        public Employee(Long id, String firstName, String lastName, String team, String email) {
            super();
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.team = team;
            this.email = email;
        }

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getTeam() {
            return team;
        }

        public void setTeam(String team) {
            this.team = team;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    // Getters and setters for main class
    public int getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(int totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public int getOfficePresence() {
        return officePresence;
    }

    public void setOfficePresence(int officePresence) {
        this.officePresence = officePresence;
    }

    public int getRemoteWork() {
        return remoteWork;
    }

    public void setRemoteWork(int remoteWork) {
        this.remoteWork = remoteWork;
    }

    public int getReservationCount() {
        return reservationCount;
    }

    public void setReservationCount(int reservationCount) {
        this.reservationCount = reservationCount;
    }

    public int getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(int occupancyRate) {
        this.occupancyRate = occupancyRate;
    }

    public double getEmployeeGrowthRate() {
        return employeeGrowthRate;
    }

    public void setEmployeeGrowthRate(double employeeGrowthRate) {
        this.employeeGrowthRate = employeeGrowthRate;
    }

    public double getOfficePresencePercentage() {
        return officePresencePercentage;
    }

    public void setOfficePresencePercentage(double officePresencePercentage) {
        this.officePresencePercentage = officePresencePercentage;
    }

    public double getOfficePresenceChange() {
        return officePresenceChange;
    }

    public void setOfficePresenceChange(double officePresenceChange) {
        this.officePresenceChange = officePresenceChange;
    }

    public List<TeamDistribution> getTeamDistribution() {
        return teamDistribution;
    }

    public void setTeamDistribution(List<TeamDistribution> teamDistribution) {
        this.teamDistribution = teamDistribution;
    }

    public List<WeeklyOccupancy> getWeeklyOccupancy() {
        return weeklyOccupancy;
    }

    public void setWeeklyOccupancy(List<WeeklyOccupancy> weeklyOccupancy) {
        this.weeklyOccupancy = weeklyOccupancy;
    }

    public ApprovalRates getApprovalRates() {
        return approvalRates;
    }

    public void setApprovalRates(ApprovalRates approvalRates) {
        this.approvalRates = approvalRates;
    }

    public List<Employee> getAllEmployees() {
        return allEmployees;
    }

    public void setAllEmployees(List<Employee> allEmployees) {
        this.allEmployees = allEmployees;
    }
}
