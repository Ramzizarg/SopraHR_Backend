package com.example.analyticsservice.model;

import java.util.List;

/**
 * Model class representing comprehensive analytics metrics for the admin dashboard
 */
public class AnalyticsMetrics {
    private int totalEmployees;
    private int officePresence;
    private int remoteWork;
    private int undefinedStatus;
    private int reservationCount;
    private int occupancyRate;
    private List<TeamDistribution> teamDistribution;
    private List<WeeklyOccupancy> weeklyOccupancy;
    private ApprovalRates approvalRates;
    private List<TopEmployee> topEmployees;

    // Nested classes for specific metric categories
    public static class TeamDistribution {
        private String team;
        private int count;

        public TeamDistribution() {}

        public TeamDistribution(String team, int count) {
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

        public WeeklyOccupancy() {}

        public WeeklyOccupancy(String day, int percentage) {
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

        public ApprovalRates() {}

        public ApprovalRates(int approved, int rejected, int pending) {
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

    public static class TopEmployee {
        private String name;
        private int days;
        private String team;

        public TopEmployee() {}

        public TopEmployee(String name, int days, String team) {
            this.name = name;
            this.days = days;
            this.team = team;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getDays() {
            return days;
        }

        public void setDays(int days) {
            this.days = days;
        }

        public String getTeam() {
            return team;
        }

        public void setTeam(String team) {
            this.team = team;
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

    public int getUndefinedStatus() {
        return undefinedStatus;
    }

    public void setUndefinedStatus(int undefinedStatus) {
        this.undefinedStatus = undefinedStatus;
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

    public List<TopEmployee> getTopEmployees() {
        return topEmployees;
    }

    public void setTopEmployees(List<TopEmployee> topEmployees) {
        this.topEmployees = topEmployees;
    }
}
