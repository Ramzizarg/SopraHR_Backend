package com.example.reservationservice.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "desks")
public class Desk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "`left`")
    private double left;
    
    @Column(name = "`top`")
    private double top;
    
    private double rotation;
    
    @ManyToOne
    @JoinColumn(name = "plan_id")
    private Plan plan;
    
    @OneToMany(mappedBy = "desk", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reservation> reservations = new ArrayList<>();

    public Desk() {
    }
    
    public Desk(double left, double top, double rotation) {
        this.left = left;
        this.top = top;
        this.rotation = rotation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getLeft() {
        return left;
    }

    public void setLeft(double left) {
        this.left = left;
    }

    public double getTop() {
        return top;
    }

    public void setTop(double top) {
        this.top = top;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }
    
    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }
    
    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
        reservation.setDesk(this);
    }
    
    public void removeReservation(Reservation reservation) {
        reservations.remove(reservation);
        reservation.setDesk(null);
    }
    
    /**
     * Check if this desk is reserved for a specific date
     * @param date the date to check in format YYYY-MM-DD
     * @return true if reserved, false otherwise
     */
    public boolean isReservedForDate(String date) {
        return reservations.stream()
                .anyMatch(reservation -> reservation.getBookingDate().equals(date));
    }
    
    /**
     * Get reservation for a specific date
     * @param date the date to check in format YYYY-MM-DD
     * @return the reservation for the date, or null if none exists
     */
    public Reservation getReservationForDate(String date) {
        return reservations.stream()
                .filter(reservation -> reservation.getBookingDate().equals(date))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Check if this desk is available for a specific date
     * @param date the date to check in format YYYY-MM-DD
     * @return true if available, false if reserved
     */
    public boolean isAvailableForDate(String date) {
        return !isReservedForDate(date);
    }
}
