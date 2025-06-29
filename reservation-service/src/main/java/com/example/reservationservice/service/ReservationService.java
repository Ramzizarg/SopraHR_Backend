package com.example.reservationservice.service;

import com.example.reservationservice.dto.ReservationDTO;
import com.example.reservationservice.exception.BadRequestException;
import com.example.reservationservice.exception.ResourceNotFoundException;
import com.example.reservationservice.exception.UnauthorizedException;
import com.example.reservationservice.model.Desk;
import com.example.reservationservice.model.Reservation;
import com.example.reservationservice.repository.DeskRepository;
import com.example.reservationservice.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final DeskRepository deskRepository;
    private final UserService userService;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository, 
                           DeskRepository deskRepository,
                           UserService userService) {
        this.reservationRepository = reservationRepository;
        this.deskRepository = deskRepository;
        this.userService = userService;
    }

    public List<ReservationDTO> getAllReservations(String token) {
        // Get all reservations if admin, otherwise only get the user's reservations
        boolean isAdmin = userService.isAdmin(token);
        Long userId = userService.getUserIdDirect(token);
        
        if (isAdmin) {
            return reservationRepository.findAll().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            return reservationRepository.findByUserId(userId).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
    }

    public ReservationDTO getReservationById(Long id, String token) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));
        
        // Check if user is admin or the owner of the reservation
        boolean isAdmin = userService.isAdmin(token);
        Long userId = userService.getUserIdDirect(token);
        
        if (!isAdmin && !reservation.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to view this reservation");
        }
        
        return convertToDTO(reservation);
    }

    public List<ReservationDTO> getReservationsByDate(String date, String token) {
        // Always return all reservations for a date to everyone, so desks show as reserved for all users
        // This is needed so regular users can see which desks are already reserved
        return reservationRepository.findByBookingDate(date).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ReservationDTO> getUserReservations(String token) {
        Long userId = userService.getUserIdDirect(token);
        return reservationRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationDTO createReservation(ReservationDTO reservationDTO, String token) {
        // Validate booking date
        validateBookingDate(reservationDTO.getBookingDate());
        
        // Get user ID and role information
        Long userId = userService.getUserIdDirect(token);
        boolean isAdmin = userService.isAdmin(token);
        
        // Log reservation attempt for debugging
        System.out.println("Reservation attempt by userId: " + userId + ", isAdmin: " + isAdmin + ", for desk: " + 
            reservationDTO.getDeskId() + ", date: " + reservationDTO.getBookingDate());
        
        // Check if user already has a reservation for this date
        List<Reservation> existingReservations = reservationRepository.findByUserIdAndBookingDate(userId, reservationDTO.getBookingDate());
        if (!existingReservations.isEmpty()) {
            throw new BadRequestException("You already have a desk reservation for this date. Only one desk reservation is allowed per day.");
        }
        
        // Get desk
        Desk desk = deskRepository.findById(reservationDTO.getDeskId())
                .orElseThrow(() -> new ResourceNotFoundException("Desk", "id", reservationDTO.getDeskId()));
        
        // Check if desk is available for the requested date
        List<Reservation> existingReservationsForDesk = reservationRepository.findReservationsByDeskAndDate(desk.getId(), reservationDTO.getBookingDate());
        if (!existingReservationsForDesk.isEmpty()) {
            // Log conflicting reservation details
            Reservation conflicting = existingReservationsForDesk.get(0);
            System.out.println("Desk unavailable - conflicting reservation: deskId=" + conflicting.getDesk().getId() + 
                ", date=" + conflicting.getBookingDate() + ", by user=" + conflicting.getUserId());
            throw new BadRequestException("Desk is already reserved for this date");
        }
        
        // Always set employee name from the authenticated user's full name (first + last name)
        String employeeName = userService.getFullNameDirect(token);
        
        // Create the reservation
        Reservation reservation = new Reservation();
        reservation.setEmployeeName(employeeName);
        reservation.setUserId(userId);
        reservation.setBookingDate(reservationDTO.getBookingDate());
        reservation.setDuration(Reservation.Duration.fromValue(reservationDTO.getDuration()));
        reservation.setDesk(desk);
        
        // Save reservation
        Reservation savedReservation = reservationRepository.save(reservation);
        System.out.println("Successfully created reservation: id=" + savedReservation.getId() + ", deskId=" + 
            savedReservation.getDesk().getId() + ", date=" + savedReservation.getBookingDate());
        
        return convertToDTO(savedReservation);
    }

    @Transactional
    public ReservationDTO updateReservation(Long id, ReservationDTO reservationDTO, String token) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));
        
        // Check if user is admin or the owner of the reservation
        boolean isAdmin = userService.isAdmin(token);
        Long userId = userService.getUserIdDirect(token);
        
        if (!isAdmin && !reservation.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this reservation");
        }
        
        // Validate booking date
        validateBookingDate(reservationDTO.getBookingDate());
        
        // Check if date is changing and if user already has a reservation for the new date (that's not this one)
        if (!reservation.getBookingDate().equals(reservationDTO.getBookingDate())) {
            List<Reservation> existingReservations = reservationRepository.findByUserIdAndBookingDate(userId, reservationDTO.getBookingDate());
            // If there's already a reservation for this date that's not the current one being updated
            boolean hasOtherReservation = existingReservations.stream()
                    .anyMatch(r -> !r.getId().equals(reservation.getId()));
            
            if (hasOtherReservation) {
                throw new BadRequestException("You already have a desk reservation for this date. Only one desk reservation is allowed per day.");
            }
        }
        
        // If changing the desk, check if the new desk is available for this date
        if (!reservation.getDesk().getId().equals(reservationDTO.getDeskId())) {
            Desk newDesk = deskRepository.findById(reservationDTO.getDeskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Desk", "id", reservationDTO.getDeskId()));
            
            // If the date is the same, we need to check if the new desk is already reserved for this date by someone else
            if (newDesk.isReservedForDate(reservationDTO.getBookingDate())) {
                // Check if the reservation for this desk and date belongs to someone else
                Reservation existingReservation = newDesk.getReservationForDate(reservationDTO.getBookingDate());
                if (existingReservation != null && !existingReservation.getId().equals(reservation.getId())) {
                    throw new BadRequestException("New desk is already reserved for this date");
                }
            }
            
            // Update reservation with new desk
            reservation.setDesk(newDesk);
            // No need to set old desk status as available - it's now determined by date
            
            // No need to set new desk status as reserved - it's now determined by date
        }
        
        // Update reservation details - always use the authenticated user's full name
        reservation.setEmployeeName(userService.getFullName(token));
        reservation.setBookingDate(reservationDTO.getBookingDate());
        reservation.setDuration(Reservation.Duration.fromValue(reservationDTO.getDuration()));
        
        Reservation updatedReservation = reservationRepository.save(reservation);
        return convertToDTO(updatedReservation);
    }

    @Transactional
    public void deleteReservation(Long id, String token) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));
        
        // Check if user is admin or the owner of the reservation
        boolean isAdmin = userService.isAdmin(token);
        Long userId = userService.getUserIdDirect(token);
        
        if (!isAdmin && !reservation.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this reservation");
        }
        
        // Update desk status
        Desk desk = reservation.getDesk();
        // No need to set desk status as available - it's now determined by date
        
        reservationRepository.delete(reservation);
    }

    /**
     * Find all reservations for a specific user in a date range
     * @param startDate the start date
     * @param endDate the end date
     * @param token the JWT token
     * @return list of reservations
     */
    public List<ReservationDTO> getUserReservationsInDateRange(String startDate, String endDate, String token) {
        Long userId = userService.getUserIdDirect(token);
        return reservationRepository.findUserReservationsInDateRange(userId, startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Validate booking date - cannot be in the past or more than 2 weeks in the future
     * @param bookingDate the booking date
     */
    private void validateBookingDate(String bookingDate) {
        LocalDate today = LocalDate.now();
        LocalDate date = LocalDate.parse(bookingDate, DateTimeFormatter.ISO_DATE);
        
        if (date.isBefore(today)) {
            throw new BadRequestException("Cannot make a reservation for a past date");
        }
        
        if (date.isAfter(today.plusDays(14))) {
            throw new BadRequestException("Cannot make a reservation more than 2 weeks in advance");
        }
    }

    /**
     * Find all reservations in a date range (for analytics)
     * @param startDate the start date
     * @param endDate the end date
     * @param token the JWT token
     * @return list of reservations
     */
    public List<ReservationDTO> getReservationsInDateRange(String startDate, String endDate, String token) {
        // Only admins can get all reservations in a date range
        boolean isAdmin = userService.isAdmin(token);
        if (!isAdmin) {
            throw new UnauthorizedException("Only administrators can view all reservations in a date range");
        }
        
        return reservationRepository.findReservationsInDateRange(startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ReservationDTO convertToDTO(Reservation reservation) {
        ReservationDTO reservationDTO = new ReservationDTO();
        reservationDTO.setId(reservation.getId());
        reservationDTO.setEmployeeName(reservation.getEmployeeName());
        reservationDTO.setUserId(reservation.getUserId());
        reservationDTO.setBookingDate(reservation.getBookingDate());
        reservationDTO.setDuration(reservation.getDuration().getValue());
        reservationDTO.setDeskId(reservation.getDesk().getId());
        reservationDTO.setCreatedAt(reservation.getCreatedAt());
        reservationDTO.setUpdatedAt(reservation.getUpdatedAt());
        return reservationDTO;
    }
}
