package com.example.reservationservice.service;

import com.example.reservationservice.dto.DeskDTO;
import com.example.reservationservice.exception.BadRequestException;
import com.example.reservationservice.exception.ResourceNotFoundException;
import com.example.reservationservice.exception.UnauthorizedException;
import com.example.reservationservice.model.Desk;
import com.example.reservationservice.model.Plan;
import com.example.reservationservice.model.Reservation;
import com.example.reservationservice.repository.DeskRepository;
import com.example.reservationservice.repository.PlanRepository;
import com.example.reservationservice.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeskService {

    private final DeskRepository deskRepository;
    private final PlanRepository planRepository;
    private final ReservationRepository reservationRepository;
    private final UserService userService;

    @Autowired
    public DeskService(DeskRepository deskRepository, 
                     PlanRepository planRepository,
                     ReservationRepository reservationRepository,
                     UserService userService) {
        this.deskRepository = deskRepository;
        this.planRepository = planRepository;
        this.reservationRepository = reservationRepository;
        this.userService = userService;
    }

    public List<DeskDTO> getAllDesks() {
        return deskRepository.findAll().stream()
                .map(desk -> convertToDTO(desk, null))
                .collect(Collectors.toList());
    }

    public DeskDTO getDeskById(Long id) {
        Desk desk = deskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Desk", "id", id));
        return convertToDTO(desk, null);
    }

    public List<DeskDTO> getDesksByPlanId(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));
        
        return deskRepository.findByPlan(plan).stream()
                .map(desk -> convertToDTO(desk, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public DeskDTO createDesk(Long planId, DeskDTO deskDTO, String token) {
        // Check if user is a manager
        if (!userService.isManager(token)) {
            throw new UnauthorizedException("Only managers can create desks");
        }

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));
        
        Desk desk = new Desk();
        desk.setLeft(deskDTO.getLeft());
        desk.setTop(deskDTO.getTop());
        desk.setRotation(deskDTO.getRotation());
        desk.setPlan(plan);
        
        Desk savedDesk = deskRepository.save(desk);
        return convertToDTO(savedDesk, null);
    }

    @Transactional
    public DeskDTO updateDesk(Long id, DeskDTO deskDTO, String token) {
        // Check if user is a manager
        if (!userService.isManager(token)) {
            throw new UnauthorizedException("Only managers can update desks");
        }

        Desk desk = deskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Desk", "id", id));
        
        desk.setLeft(deskDTO.getLeft());
        desk.setTop(deskDTO.getTop());
        desk.setRotation(deskDTO.getRotation());
        
        Desk updatedDesk = deskRepository.save(desk);
        return convertToDTO(updatedDesk, null);
    }

    @Transactional
    public void deleteDesk(Long id, String token) {
        // Check if user is a manager
        if (!userService.isManager(token)) {
            throw new UnauthorizedException("Only managers can delete desks");
        }

        Desk desk = deskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Desk", "id", id));
        
        // Check if desk has any reservations
        if (!desk.getReservations().isEmpty()) {
            throw new BadRequestException("Cannot delete a desk that has reservations");
        }
        
        deskRepository.delete(desk);
    }

    /**
     * Check if a desk is available for booking on a specific date
     * @param deskId the desk ID
     * @param date the booking date
     * @return true if the desk is available, false otherwise
     */
    public boolean isDeskAvailableForDate(Long deskId, String date) {
        Desk desk = deskRepository.findById(deskId)
                .orElseThrow(() -> new ResourceNotFoundException("Desk", "id", deskId));
        
        // Check if there are any reservations for this desk on the given date
        List<Reservation> reservations = reservationRepository.findReservationsByDeskAndDate(deskId, date);
        return reservations.isEmpty();
    }

    /**
     * Get desk availability status for a specific date
     * @param deskId the desk ID
     * @param date the booking date
     * @return the desk with availability status
     */
    public DeskDTO getDeskAvailabilityForDate(Long deskId, String date) {
        Desk desk = deskRepository.findById(deskId)
                .orElseThrow(() -> new ResourceNotFoundException("Desk", "id", deskId));
        
        return convertToDTO(desk, date);
    }

    /**
     * Find all available desks for a specific date
     * @param planId the plan ID
     * @param date the booking date
     * @return list of available desks
     */
    public List<DeskDTO> getAvailableDesksByPlanAndDate(Long planId, String date) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));
        
        List<Desk> allDesks = deskRepository.findByPlan(plan);
        List<Reservation> reservationsForDate = reservationRepository.findReservationsByPlanAndDate(planId, date);
        
        // Get IDs of reserved desks
        List<Long> reservedDeskIds = reservationsForDate.stream()
                .map(r -> r.getDesk().getId())
                .collect(Collectors.toList());
        
        // Filter out reserved desks
        return allDesks.stream()
                .filter(desk -> !reservedDeskIds.contains(desk.getId()))
                .map(desk -> convertToDTO(desk, date))
                .collect(Collectors.toList());
    }

    private DeskDTO convertToDTO(Desk desk) {
        DeskDTO deskDTO = new DeskDTO();
        deskDTO.setId(desk.getId());
        deskDTO.setLeft(desk.getLeft());
        deskDTO.setTop(desk.getTop());
        deskDTO.setRotation(desk.getRotation());
        deskDTO.setAvailable(true); // By default, desks are available
        return deskDTO;
    }
    
    /**
     * Convert a Desk entity to a DeskDTO with availability for a specific date
     * @param desk the desk entity to convert
     * @param date the date to check availability for
     * @return the DTO with availability set for the given date
     */
    private DeskDTO convertToDTO(Desk desk, String date) {
        DeskDTO deskDTO = new DeskDTO();
        deskDTO.setId(desk.getId());
        deskDTO.setLeft(desk.getLeft());
        deskDTO.setTop(desk.getTop());
        deskDTO.setRotation(desk.getRotation());
        
        // Check if desk is available for this date
        boolean isAvailable = date == null || !desk.isReservedForDate(date);
        deskDTO.setAvailable(isAvailable);
        
        // If desk is reserved for this date, include reservation info
        if (!isAvailable && date != null) {
            Reservation reservation = desk.getReservationForDate(date);
            if (reservation != null) {
                deskDTO.setEmployeeName(reservation.getEmployeeName());
                deskDTO.setDuration(reservation.getDuration().getValue());
                deskDTO.setBookingDate(reservation.getBookingDate());
            }
        }
        
        return deskDTO;
    }
}
