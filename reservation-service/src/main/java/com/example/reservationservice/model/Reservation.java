package com.example.reservationservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String employeeName;
    private Long userId;
    
    private String bookingDate;
    
    @Enumerated(EnumType.STRING)
    private Duration duration;
    
    @ManyToOne
    @JoinColumn(name = "desk_id")
    private Desk desk;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Reservation() {
        super();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Reservation(String employeeName, Long userId, String bookingDate, Duration duration) {
        super();
        this.employeeName = employeeName;
        this.userId = userId;
        this.bookingDate = bookingDate;
        this.duration = duration;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public Desk getDesk() {
        return desk;
    }

    public void setDesk(Desk desk) {
        this.desk = desk;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum Duration {
        AM("AM"),
        PM("PM"),
        FULL("FULL");
        
        private final String value;
        
        Duration(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static Duration fromValue(String value) {
            for (Duration duration : Duration.values()) {
                if (duration.getValue().equalsIgnoreCase(value)) {
                    return duration;
                }
            }
            throw new IllegalArgumentException("Invalid duration value: " + value);
        }
        // Helper: does this duration overlap with another?
        public boolean overlaps(Duration other) {
            if (this == FULL || other == FULL) return true;
            return this == other;
        }
    }
}
