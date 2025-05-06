package com.example.reservationservice.service;

import com.example.reservationservice.dto.PlanDTO;
import com.example.reservationservice.dto.DeskDTO;
import com.example.reservationservice.dto.WallDTO;
import com.example.reservationservice.exception.BadRequestException;
import com.example.reservationservice.exception.ResourceNotFoundException;
import com.example.reservationservice.exception.UnauthorizedException;
import com.example.reservationservice.model.Desk;
import com.example.reservationservice.model.Plan;
import com.example.reservationservice.model.Reservation;
import com.example.reservationservice.model.Wall;
import com.example.reservationservice.repository.PlanRepository;
import com.example.reservationservice.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final ReservationRepository reservationRepository;
    private final UserService userService;

    @Autowired
    public PlanService(PlanRepository planRepository, 
                      ReservationRepository reservationRepository,
                      UserService userService) {
        this.planRepository = planRepository;
        this.reservationRepository = reservationRepository;
        this.userService = userService;
    }

    public List<PlanDTO> getAllPlans() {
        return planRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PlanDTO getPlanById(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
        return convertToDTO(plan);
    }

    public PlanDTO getPlanByPlanId(String planId) {
        Plan plan = planRepository.findByPlanId(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "planId", planId));
        return convertToDTO(plan);
    }

    @Transactional
    public PlanDTO createPlan(PlanDTO planDTO, String token) {
        // Check if user is a manager
        if (!userService.isManager(token)) {
            throw new UnauthorizedException("Only managers can create plans");
        }
        
        // Check if a plan already exists
        List<Plan> existingPlans = planRepository.findAll();
        if (!existingPlans.isEmpty()) {
            throw new BadRequestException("A plan already exists. Only one plan is allowed. Delete the existing plan first.");
        }

        Plan plan = new Plan();
        plan.setPlanId("plan-" + UUID.randomUUID().toString().substring(0, 8));
        plan.setLeft(planDTO.getLeft());
        plan.setTop(planDTO.getTop());
        plan.setWidth(planDTO.getWidth());
        plan.setHeight(planDTO.getHeight());
        
        Plan savedPlan = planRepository.save(plan);
        return convertToDTO(savedPlan);
    }

    @Transactional
    public PlanDTO updatePlan(Long id, PlanDTO planDTO, String token) {
        // Check if user is a manager
        if (!userService.isManager(token)) {
            throw new UnauthorizedException("Only managers can update plans");
        }

        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
        
        plan.setLeft(planDTO.getLeft());
        plan.setTop(planDTO.getTop());
        plan.setWidth(planDTO.getWidth());
        plan.setHeight(planDTO.getHeight());
        
        Plan updatedPlan = planRepository.save(plan);
        return convertToDTO(updatedPlan);
    }

    @Transactional
    public void deletePlan(Long id, String token) {
        // Check if user is a manager
        if (!userService.isManager(token)) {
            throw new UnauthorizedException("Only managers can delete plans");
        }

        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
        
        planRepository.delete(plan);
    }

    @Transactional
    public PlanDTO updateFullPlan(Long id, PlanDTO planDTO, String token) {
        // Check if user is a manager
        if (!userService.isManager(token)) {
            throw new UnauthorizedException("Only managers can update plans");
        }

        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
        
        plan.setLeft(planDTO.getLeft());
        plan.setTop(planDTO.getTop());
        plan.setWidth(planDTO.getWidth());
        plan.setHeight(planDTO.getHeight());
        
        // Clear existing desks and walls
        plan.getDesks().clear();
        plan.getWalls().clear();
        
        // Add new desks
        if (planDTO.getDesks() != null) {
            for (DeskDTO deskDTO : planDTO.getDesks()) {
                Desk desk = new Desk();
                desk.setLeft(deskDTO.getLeft());
                desk.setTop(deskDTO.getTop());
                desk.setRotation(deskDTO.getRotation());
                plan.addDesk(desk);
            }
        }
        
        // Add new walls
        if (planDTO.getWalls() != null) {
            for (WallDTO wallDTO : planDTO.getWalls()) {
                Wall wall = new Wall();
                wall.setWallId(wallDTO.getWallId() != null ? wallDTO.getWallId() : "wall-" + UUID.randomUUID().toString().substring(0, 8));
                wall.setLeft(wallDTO.getLeft());
                wall.setTop(wallDTO.getTop());
                wall.setWidth(wallDTO.getWidth());
                wall.setHeight(wallDTO.getHeight());
                plan.addWall(wall);
            }
        }
        
        Plan updatedPlan = planRepository.save(plan);
        return convertToDTO(updatedPlan);
    }

    public PlanDTO getPlanWithAvailabilityByDate(Long id, String date) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
        
        PlanDTO planDTO = convertToDTO(plan);
        
        // Get all reservations for this plan for the given date
        List<Reservation> reservations = reservationRepository.findReservationsByPlanAndDate(id, date);
        
        // Update desk status based on reservations
        for (DeskDTO deskDTO : planDTO.getDesks()) {
            boolean isReserved = reservations.stream()
                    .anyMatch(r -> r.getDesk().getId().equals(deskDTO.getId()));
            
            if (isReserved) {
                deskDTO.setAvailable(false);
                
                // Find the reservation for this desk
                Reservation reservation = reservations.stream()
                        .filter(r -> r.getDesk().getId().equals(deskDTO.getId()))
                        .findFirst()
                        .orElse(null);
                
                if (reservation != null) {
                    deskDTO.setEmployeeName(reservation.getEmployeeName());
                    deskDTO.setDuration(reservation.getDuration().getValue());
                    deskDTO.setBookingDate(reservation.getBookingDate());
                }
            } else {
                deskDTO.setAvailable(true);
            }
        }
        
        return planDTO;
    }

    private PlanDTO convertToDTO(Plan plan) {
        PlanDTO planDTO = new PlanDTO();
        planDTO.setId(plan.getId());
        planDTO.setPlanId(plan.getPlanId());
        planDTO.setLeft(plan.getLeft());
        planDTO.setTop(plan.getTop());
        planDTO.setWidth(plan.getWidth());
        planDTO.setHeight(plan.getHeight());
        
        // Convert desks
        planDTO.setDesks(plan.getDesks().stream()
                .map(desk -> {
                    DeskDTO deskDTO = new DeskDTO();
                    deskDTO.setId(desk.getId());
                    deskDTO.setLeft(desk.getLeft());
                    deskDTO.setTop(desk.getTop());
                    deskDTO.setRotation(desk.getRotation());
                    deskDTO.setAvailable(true); // All desks are available by default
                    return deskDTO;
                })
                .collect(Collectors.toList()));
        
        // Convert walls
        planDTO.setWalls(plan.getWalls().stream()
                .map(wall -> {
                    WallDTO wallDTO = new WallDTO();
                    wallDTO.setId(wall.getId());
                    wallDTO.setWallId(wall.getWallId());
                    wallDTO.setLeft(wall.getLeft());
                    wallDTO.setTop(wall.getTop());
                    wallDTO.setWidth(wall.getWidth());
                    wallDTO.setHeight(wall.getHeight());
                    return wallDTO;
                })
                .collect(Collectors.toList()));
        
        return planDTO;
    }
}
