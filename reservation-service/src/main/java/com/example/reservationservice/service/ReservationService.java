package com.example.reservationservice.service;

import com.example.reservationservice.dto.BookingRequestDTO;
import com.example.reservationservice.dto.DeskDTO;
import com.example.reservationservice.dto.PlanDTO;
import com.example.reservationservice.dto.WallDTO;
import com.example.reservationservice.entity.Booking;
import com.example.reservationservice.entity.Desk;
import com.example.reservationservice.entity.Plan;
import com.example.reservationservice.entity.Wall;
import com.example.reservationservice.repository.BookingRepository;
import com.example.reservationservice.repository.DeskRepository;
import com.example.reservationservice.repository.PlanRepository;
import com.example.reservationservice.repository.WallRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ReservationService {
    private final PlanRepository planRepository;
    private final DeskRepository deskRepository;
    private final WallRepository wallRepository;
    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate;
    private final String userserviceUrl;

    public ReservationService(PlanRepository planRepository,
                              DeskRepository deskRepository,
                              WallRepository wallRepository,
                              BookingRepository bookingRepository,
                              RestTemplate restTemplate,
                              @Value("${userservice.url}") String userserviceUrl) {
        this.planRepository = planRepository;
        this.deskRepository = deskRepository;
        this.wallRepository = wallRepository;
        this.bookingRepository = bookingRepository;
        this.restTemplate = restTemplate;
        this.userserviceUrl = userserviceUrl;
    }

    @Transactional
    public PlanDTO createPlan(PlanDTO planDTO, String email) {
        if (planDTO == null || planDTO.getId() == null) {
            throw new IllegalArgumentException("PlanDTO or plan ID cannot be null");
        }

        Long userId = getUserIdByEmail(email);
        if (userId == null) {
            throw new IllegalArgumentException("User not found for email: " + email);
        }

        Plan plan = new Plan();
        plan.setPlanId(planDTO.getId());
        plan.setXPosition(planDTO.getLeft());
        plan.setTop(planDTO.getTop());
        plan.setWidth(planDTO.getWidth());
        plan.setHeight(planDTO.getHeight());

        List<Desk> desks = new ArrayList<>();
        for (DeskDTO deskDTO : planDTO.getDesks()) {
            if (deskDTO.getId() == null) {
                throw new IllegalArgumentException("Desk ID cannot be null");
            }
            Desk desk = new Desk();
            desk.setDeskId(deskDTO.getId());
            desk.setXPosition(deskDTO.getLeft());
            desk.setTop(deskDTO.getTop());
            desk.setRotation(deskDTO.getRotation());
            desk.setPlan(plan);
            desks.add(desk);
        }
        plan.setDesks(desks);

        List<Wall> walls = new ArrayList<>();
        for (WallDTO wallDTO : planDTO.getWalls()) {
            if (wallDTO.getId() == null) {
                throw new IllegalArgumentException("Wall ID cannot be null");
            }
            Wall wall = new Wall();
            wall.setWallId(wallDTO.getId());
            wall.setXPosition(wallDTO.getLeft());
            wall.setTop(wallDTO.getTop());
            wall.setWidth(wallDTO.getWidth());
            wall.setHeight(wallDTO.getHeight());
            wall.setRotation(wallDTO.getRotation());
            wall.setPlan(plan);
            walls.add(wall);
        }
        plan.setWalls(walls);

        plan = planRepository.save(plan);
        return toPlanDTO(plan);
    }

    @Transactional
    public void deletePlan(String planId) {
        if (planId == null) {
            throw new IllegalArgumentException("Plan ID cannot be null");
        }
        planRepository.findByPlanId(planId)
                .ifPresentOrElse(
                        planRepository::delete,
                        () -> {
                            throw new IllegalArgumentException("Plan not found: " + planId);
                        });
    }

    @Transactional(readOnly = true)
    public List<PlanDTO> getAllPlans() {
        return planRepository.findAll().stream()
                .map(this::toPlanDTO)
                .toList();
    }

    @Transactional
    public PlanDTO updatePlan(PlanDTO planDTO) {
        if (planDTO == null || planDTO.getId() == null) {
            throw new IllegalArgumentException("PlanDTO or plan ID cannot be null");
        }

        Plan plan = planRepository.findByPlanId(planDTO.getId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planDTO.getId()));

        plan.setXPosition(planDTO.getLeft());
        plan.setTop(planDTO.getTop());
        plan.setWidth(planDTO.getWidth());
        plan.setHeight(planDTO.getHeight());

        plan.getDesks().clear();
        for (DeskDTO deskDTO : planDTO.getDesks()) {
            if (deskDTO.getId() == null) {
                throw new IllegalArgumentException("Desk ID cannot be null");
            }
            Desk desk = new Desk();
            desk.setDeskId(deskDTO.getId());
            desk.setXPosition(deskDTO.getLeft());
            desk.setTop(deskDTO.getTop());
            desk.setRotation(deskDTO.getRotation());
            desk.setPlan(plan);
            plan.getDesks().add(desk);
        }

        plan.getWalls().clear();
        for (WallDTO wallDTO : planDTO.getWalls()) {
            if (wallDTO.getId() == null) {
                throw new IllegalArgumentException("Wall ID cannot be null");
            }
            Wall wall = new Wall();
            wall.setWallId(wallDTO.getId());
            wall.setXPosition(wallDTO.getLeft());
            wall.setTop(wallDTO.getTop());
            wall.setWidth(wallDTO.getWidth());
            wall.setHeight(wallDTO.getHeight());
            wall.setRotation(wallDTO.getRotation());
            wall.setPlan(plan);
            plan.getWalls().add(wall);
        }

        plan = planRepository.save(plan);
        return toPlanDTO(plan);
    }

    @Transactional
    public void bookDesk(BookingRequestDTO request, String email) {
        if (request == null || request.getDeskId() == null || request.getBookingDates() == null) {
            throw new IllegalArgumentException("Booking request or required fields cannot be null");
        }

        Long userId = getUserIdByEmail(email);
        if (userId == null) {
            throw new IllegalArgumentException("User not found for email: " + email);
        }

        Desk desk = deskRepository.findByDeskId(request.getDeskId())
                .orElseThrow(() -> new IllegalArgumentException("Desk not found: " + request.getDeskId()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (String dateStr : request.getBookingDates()) {
            LocalDate bookingDate;
            try {
                bookingDate = LocalDate.parse(dateStr, formatter);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date format: " + dateStr);
            }

            List<Booking> existingBookings = bookingRepository.findByDeskAndBookingDate(desk, bookingDate);
            if (!existingBookings.isEmpty()) {
                throw new IllegalArgumentException("Desk already booked on " + dateStr);
            }

            Booking booking = new Booking();
            booking.setUserId(userId);
            booking.setEmployeeName(request.getEmployeeName());
            booking.setBookingDate(bookingDate);
            booking.setDuration(request.getDuration());
            booking.setDesk(desk);
            bookingRepository.save(booking);
        }
    }

    @Transactional
    public void cancelBooking(Long deskId, String bookingDate) {
        if (deskId == null || bookingDate == null) {
            throw new IllegalArgumentException("Desk ID or booking date cannot be null");
        }

        Desk desk = deskRepository.findByDeskId(deskId)
                .orElseThrow(() -> new IllegalArgumentException("Desk not found: " + deskId));

        LocalDate date;
        try {
            date = LocalDate.parse(bookingDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + bookingDate);
        }

        List<Booking> bookings = bookingRepository.findByDeskAndBookingDate(desk, date);
        if (bookings.isEmpty()) {
            throw new IllegalArgumentException("No bookings found for desk " + deskId + " on " + bookingDate);
        }
        bookingRepository.deleteAll(bookings);
    }

    private PlanDTO toPlanDTO(Plan plan) {
        PlanDTO planDTO = new PlanDTO();
        planDTO.setId(plan.getPlanId());
        planDTO.setLeft(plan.getXPosition());
        planDTO.setTop(plan.getTop());
        planDTO.setWidth(plan.getWidth());
        planDTO.setHeight(plan.getHeight());

        List<DeskDTO> deskDTOs = new ArrayList<>();
        for (Desk desk : plan.getDesks()) {
            DeskDTO deskDTO = new DeskDTO();
            deskDTO.setId(desk.getDeskId());
            deskDTO.setLeft(desk.getXPosition());
            deskDTO.setTop(desk.getTop());
            deskDTO.setRotation(desk.getRotation());

            LocalDate currentDate = LocalDate.now();
            List<Booking> bookings = bookingRepository.findByDeskAndBookingDate(desk, currentDate);
            if (!bookings.isEmpty()) {
                Booking booking = bookings.get(0);
                deskDTO.setStatus("reserved");
                deskDTO.setEmployeeName(booking.getEmployeeName());
                deskDTO.setDuration(booking.getDuration());
                deskDTO.setBookingDate(booking.getBookingDate().toString());
            } else {
                deskDTO.setStatus("available");
            }
            deskDTOs.add(deskDTO);
        }
        planDTO.setDesks(deskDTOs);

        List<WallDTO> wallDTOs = plan.getWalls().stream()
                .map(wall -> {
                    WallDTO wallDTO = new WallDTO();
                    wallDTO.setId(wall.getWallId());
                    wallDTO.setLeft(wall.getXPosition());
                    wallDTO.setTop(wall.getTop());
                    wallDTO.setWidth(wall.getWidth());
                    wallDTO.setHeight(wall.getHeight());
                    wallDTO.setRotation(wall.getRotation());
                    return wallDTO;
                })
                .toList();
        planDTO.setWalls(wallDTOs);

        return planDTO;
    }

    private Long getUserIdByEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        try {
            String url = userserviceUrl + "/validate-by-email/" + email;
            Map<String, Long> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !response.containsKey("id")) {
                log.error("Invalid response from userservice for email: {}", email);
                return null;
            }
            return response.get("id");
        } catch (Exception e) {
            log.error("Failed to fetch userId for email {}: {}", email, e.getMessage());
            throw new RuntimeException("Userservice unavailable", e);
        }
    }
}