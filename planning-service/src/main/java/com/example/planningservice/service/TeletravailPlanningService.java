package com.example.planningservice.service;

import com.example.planningservice.client.TeletravailClient;
import com.example.planningservice.dto.PlanningGenerationRequest;
import com.example.planningservice.dto.PlanningResponseDTO;
import com.example.planningservice.dto.TeletravailRequestDTO;
import com.example.planningservice.entity.TeletravailPlanning;
import com.example.planningservice.repository.TeletravailPlanningRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class TeletravailPlanningService {

    private final TeletravailPlanningRepository planningRepository;
    private final TeletravailClient teletravailClient;
    private final UserService userService;

    // Maximum number of teletravail days per week
    private static final int MAX_DAYS_PER_WEEK = 2;
    // Maximum consecutive days
    private static final int MAX_CONSECUTIVE_DAYS = 1;

    @Autowired
    public TeletravailPlanningService(
            TeletravailPlanningRepository planningRepository,
            TeletravailClient teletravailClient,
            UserService userService) {
        this.planningRepository = planningRepository;
        this.teletravailClient = teletravailClient;
        this.userService = userService;
    }

    /**
     * Sync a teletravail request with planning
     * This method is called when a teletravail request is created, updated, or deleted
     * It creates or updates a planning entry based on the teletravail request
     * 
     * @param request The teletravail request to sync with planning
     * @return true if a planning entry was created or updated, false otherwise
     */
    public boolean syncTeletravailWithPlanning(TeletravailRequestDTO request) {
        if (request == null || request.getUserId() == null || request.getTeletravailDate() == null) {
            log.warn("Invalid teletravail request received for sync: {}", request);
            return false;
        }

        LocalDate planningDate = LocalDate.parse(request.getTeletravailDate());
        Long userId = request.getUserId();
        
        log.info("Syncing teletravail request for user {} on date {}", userId, planningDate);
        
        // Fetch the full user name (first + last name)
        String userName = userService.getUserNameById(userId);
        log.info("Retrieved full user name: {} for user ID: {}", userName, userId);
        
        // Check if a planning entry already exists for this date and user
        Optional<TeletravailPlanning> existingPlanning = planningRepository
                .findByUserIdAndPlanningDate(userId, planningDate);
        
        if (existingPlanning.isPresent()) {
            log.info("Updating existing planning entry for user {} on date {}", userId, planningDate);
            TeletravailPlanning planning = existingPlanning.get();
            
            // Always update the user name to ensure it's current
            planning.setUserName(userName);
            
            // Set location based on request
            String location = "Domicile";
            if ("non".equalsIgnoreCase(request.getTravailMaison())) {
                location = request.getSelectedPays() + " - " + request.getSelectedGouvernorat();
            }
            planning.setLocation(location);
            
            // Set workType based on the telework request type
            String workType = "Regular";
            if ("exceptionnel".equalsIgnoreCase(request.getTravailType())) {
                workType = "Exceptional";
            }
            planning.setWorkType(workType);
            
            planning.setReasons(request.getReason());
            
            planningRepository.save(planning);
            return true;
        } else {
            log.info("Creating new planning entry for user {} on date {}", userId, planningDate);
            
            // Create new planning
            TeletravailPlanning planning = new TeletravailPlanning();
            planning.setUserId(userId);
            planning.setUserName(userName);
            planning.setPlanningDate(planningDate);
            planning.setPlanningStatus("PLANNED");
            
            // Set location based on request
            String location = "Domicile";
            if ("non".equalsIgnoreCase(request.getTravailMaison())) {
                location = request.getSelectedPays() + " - " + request.getSelectedGouvernorat();
            }
            planning.setLocation(location);
            
            // Set workType based on the telework request type
            String workType = "Regular";
            if ("exceptionnel".equalsIgnoreCase(request.getTravailType())) {
                workType = "Exceptional";
            }
            planning.setWorkType(workType);
            
            planning.setReasons(request.getReason());
            
            planningRepository.save(planning);
            return true;
        }
    }

    /**
     * Generate a planning based on teletravail requests for a specific period
     */
    @CircuitBreaker(name = "teletravailService", fallbackMethod = "generatePlanningFallback")
    public List<PlanningResponseDTO> generatePlanning(PlanningGenerationRequest request) {
        log.info("Generating planning from {} to {}", request.getStartDate(), request.getEndDate());
        
        // Get all teletravail requests
        List<TeletravailRequestDTO> allRequests = teletravailClient.getAllTeletravailRequests();
        
        // Filter requests by date range
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        
        List<TeletravailRequestDTO> filteredRequests = allRequests.stream()
                .filter(r -> {
                    LocalDate requestDate = LocalDate.parse(r.getTeletravailDate());
                    return !requestDate.isBefore(startDate) && !requestDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
        
        log.info("Found {} teletravail requests in the specified date range", filteredRequests.size());
        
        // Group requests by user
        Map<Long, List<TeletravailRequestDTO>> requestsByUser = filteredRequests.stream()
                .collect(Collectors.groupingBy(TeletravailRequestDTO::getUserId));
        
        List<TeletravailPlanning> plannings = new ArrayList<>();
        
        // Process each user's requests
        for (Map.Entry<Long, List<TeletravailRequestDTO>> entry : requestsByUser.entrySet()) {
            Long userId = entry.getKey();
            List<TeletravailRequestDTO> userRequests = entry.getValue();
            
            String userName = userService.getUserNameById(userId);
            
            for (TeletravailRequestDTO requestDTO : userRequests) {
                LocalDate planningDate = LocalDate.parse(requestDTO.getTeletravailDate());
                
                // Check if a planning already exists for this date and user
                if (planningRepository.existsByUserIdAndPlanningDate(userId, planningDate)) {
                    log.info("Planning already exists for user {} on date {}", userId, planningDate);
                    continue;
                }
                
                // Create new planning
                TeletravailPlanning planning = new TeletravailPlanning();
                planning.setUserId(userId);
                planning.setUserName(userName);
                planning.setPlanningDate(planningDate);
                planning.setPlanningStatus("PLANNED");
                
                // Set location based on request
                String location = "Domicile";
                if ("non".equalsIgnoreCase(requestDTO.getTravailMaison())) {
                    location = requestDTO.getSelectedPays() + " - " + requestDTO.getSelectedGouvernorat();
                }
                planning.setLocation(location);
                
                // Set workType based on the telework request type
                String workType = "Regular";
                if ("exceptionnel".equalsIgnoreCase(requestDTO.getTravailType())) {
                    workType = "Exceptional";
                }
                planning.setWorkType(workType);
                
                log.info("Creating planning entry for user {} on date {} with type: {}", 
                         userId, planningDate, workType);
                
                planning.setReasons(requestDTO.getReason());
                
                plannings.add(planning);
            }
        }
        
        // Save all plannings
        List<TeletravailPlanning> savedPlannings = planningRepository.saveAll(plannings);
        log.info("Saved {} new planning entries", savedPlannings.size());
        
        // Convert to response DTOs
        return savedPlannings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Generate a planning automatically for a specific user based on rules
     */
    public List<PlanningResponseDTO> generateAutomaticPlanning(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating automatic planning for user {} from {} to {}", userId, startDate, endDate);
        
        // Check if user exists
        String userName = userService.getUserNameById(userId);
        if (userName == null) {
            throw new IllegalArgumentException("User with ID " + userId + " not found");
        }
        
        // Get all existing teletravail days for this user in the period
        List<TeletravailPlanning> existingPlannings = planningRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        
        // Create a set of dates that are already planned
        Set<LocalDate> existingDates = existingPlannings.stream()
                .map(TeletravailPlanning::getPlanningDate)
                .collect(Collectors.toSet());
        
        List<TeletravailPlanning> newPlannings = new ArrayList<>();
        
        // Generate planning for each week in the range
        LocalDate currentWeekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekStart = endDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        while (!currentWeekStart.isAfter(lastWeekStart)) {
            // For each week, try to schedule MAX_DAYS_PER_WEEK days
            List<LocalDate> weekDates = generateOptimalDaysForWeek(userId, currentWeekStart, existingDates);
            
            for (LocalDate date : weekDates) {
                TeletravailPlanning planning = new TeletravailPlanning();
                planning.setUserId(userId);
                planning.setUserName(userName);
                planning.setPlanningDate(date);
                planning.setPlanningStatus("PLANNED");
                planning.setLocation("Domicile");
                planning.setWorkType("reguliere");
                planning.setReasons("Générée automatiquement");
                
                newPlannings.add(planning);
                existingDates.add(date); // Add to existing dates to prevent duplicates
            }
            
            // Move to next week
            currentWeekStart = currentWeekStart.plusWeeks(1);
        }
        
        // Save all new plannings
        List<TeletravailPlanning> savedPlannings = planningRepository.saveAll(newPlannings);
        log.info("Saved {} automatically generated planning entries for user {}", savedPlannings.size(), userId);
        
        // Convert to response DTOs
        return savedPlannings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Generate optimal days for teletravail in a week
     */
    private List<LocalDate> generateOptimalDaysForWeek(Long userId, LocalDate weekStart, Set<LocalDate> existingDates) {
        List<LocalDate> result = new ArrayList<>();
        int daysToSchedule = MAX_DAYS_PER_WEEK;
        
        // Default optimal days are Tuesday and Thursday
        List<DayOfWeek> optimalDays = Arrays.asList(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
        
        // Try to schedule on optimal days first
        for (DayOfWeek dayOfWeek : optimalDays) {
            if (daysToSchedule <= 0) {
                break;
            }
            
            LocalDate date = weekStart.with(TemporalAdjusters.nextOrSame(dayOfWeek));
            
            // Skip if date is outside working days or already scheduled
            if (date.getDayOfWeek().getValue() > 5 || existingDates.contains(date)) {
                continue;
            }
            
            // Check if this would create consecutive days
            boolean isConsecutive = false;
            for (int i = 1; i <= MAX_CONSECUTIVE_DAYS; i++) {
                if (existingDates.contains(date.minusDays(i)) || existingDates.contains(date.plusDays(i))) {
                    isConsecutive = true;
                    break;
                }
            }
            
            if (!isConsecutive) {
                result.add(date);
                daysToSchedule--;
            }
        }
        
        // If we still need to schedule more days, try other weekdays
        if (daysToSchedule > 0) {
            for (int i = 1; i <= 5; i++) {
                if (daysToSchedule <= 0) {
                    break;
                }
                
                DayOfWeek day = DayOfWeek.of(i);
                // Skip already tried optimal days
                if (optimalDays.contains(day)) {
                    continue;
                }
                
                LocalDate date = weekStart.with(TemporalAdjusters.nextOrSame(day));
                
                // Skip if date is already scheduled
                if (existingDates.contains(date)) {
                    continue;
                }
                
                // Check if this would create consecutive days
                boolean isConsecutive = false;
                for (int j = 1; j <= MAX_CONSECUTIVE_DAYS; j++) {
                    if (existingDates.contains(date.minusDays(j)) || existingDates.contains(date.plusDays(j))) {
                        isConsecutive = true;
                        break;
                    }
                }
                
                if (!isConsecutive) {
                    result.add(date);
                    daysToSchedule--;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Get planning for a specific user in a date range
     */
    public List<PlanningResponseDTO> getUserPlanning(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Retrieving planning for user {} from {} to {}", userId, startDate, endDate);
        
        List<TeletravailPlanning> plannings = planningRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        
        return plannings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all planning entries for a date range
     */
    public List<PlanningResponseDTO> getAllPlanning(LocalDate startDate, LocalDate endDate) {
        log.info("Retrieving all planning from {} to {}", startDate, endDate);
        
        List<TeletravailPlanning> plannings = planningRepository.findByDateRange(startDate, endDate);
        
        return plannings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Update planning status
     */
    public PlanningResponseDTO updatePlanningStatus(Long planningId, String newStatus) {
        log.info("Updating planning {} to status: {}", planningId, newStatus);
        
        TeletravailPlanning planning = planningRepository.findById(planningId)
                .orElseThrow(() -> new IllegalArgumentException("Planning not found with ID: " + planningId));
        
        planning.setPlanningStatus(newStatus);
        TeletravailPlanning updatedPlanning = planningRepository.save(planning);
        
        return convertToDTO(updatedPlanning);
    }
    
    /**
     * Delete planning
     */
    public void deletePlanning(Long planningId) {
        log.info("Deleting planning {}", planningId);
        
        if (!planningRepository.existsById(planningId)) {
            throw new IllegalArgumentException("Planning not found with ID: " + planningId);
        }
        
        planningRepository.deleteById(planningId);
    }
    
    /**
     * Convert planning entity to response DTO
     */
    private PlanningResponseDTO convertToDTO(TeletravailPlanning planning) {
        PlanningResponseDTO dto = new PlanningResponseDTO();
        dto.setId(planning.getId());
        dto.setUserId(planning.getUserId());
        dto.setUserName(planning.getUserName());
        dto.setPlanningDate(planning.getPlanningDate());
        dto.setPlanningStatus(planning.getPlanningStatus());
        dto.setLocation(planning.getLocation());
        dto.setWorkType(planning.getWorkType());
        dto.setReasons(planning.getReasons());
        dto.setCreatedAt(planning.getCreatedAt());
        dto.setUpdatedAt(planning.getUpdatedAt());
        return dto;
    }
    /**
     * Fallback method for circuit breaker
     */
    public List<PlanningResponseDTO> generatePlanningFallback(PlanningGenerationRequest request, Exception e) {
        log.error("Failed to generate planning: {}", e.getMessage());
        return Collections.emptyList();
    }
    
    /**
     * Check if planning exists for a specific user in a date range
     * 
     * @param userId The user ID to check
     * @param startDate Start date of range
     * @param endDate End date of range
     * @return true if planning exists, false otherwise
     */
    public boolean checkIfPlanningExistsForUser(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Checking if planning exists for user {} between {} and {}", userId, startDate, endDate);
        return planningRepository.existsByUserIdAndPlanningDateBetween(userId, startDate, endDate);
    }
    
    /**
     * Generate planning for a specific user
     * Used when automatically updating planning after telework request submission
     * 
     * @param userId The user ID
     * @param startDate Start date of range
     * @param endDate End date of range
     * @return List of planning response DTOs
     */
    @Transactional
    @CircuitBreaker(name = "teletravailService", fallbackMethod = "generatePlanningForUserFallback")
    public List<PlanningResponseDTO> generatePlanningForUser(Long userId, LocalDate startDate, LocalDate endDate, String authHeader) {
        log.info("Generating planning for user {} from {} to {}", userId, startDate, endDate);
        
        // Log the auth token presence
        log.info("Using auth token for teletravail-service call: {}", authHeader != null ? "present" : "missing");
        
        // Get all teletravail requests for this user, directly passing the auth header
        List<TeletravailRequestDTO> userRequests = teletravailClient.getUserTeletravailRequests(userId, authHeader);
        
        // Log all requests received to diagnose the issue
        log.info("Received {} telework requests from teletravail-service for user {}", userRequests.size(), userId);
        userRequests.forEach(req -> {
            log.info("Telework request: id={}, userId={}, type={}, date={}", 
                    req.getId(), req.getUserId(), req.getTravailType(), req.getTeletravailDate());
        });
        
        // Filter requests by date range
        List<TeletravailRequestDTO> filteredRequests = userRequests.stream()
                .filter(r -> {
                    LocalDate requestDate = LocalDate.parse(r.getTeletravailDate());
                    return !requestDate.isBefore(startDate) && !requestDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
        
        log.info("Found {} teletravail requests in the specified date range for user {}", filteredRequests.size(), userId);
        
        String userName = userService.getUserNameById(userId);
        List<TeletravailPlanning> plannings = new ArrayList<>();
        
        for (TeletravailRequestDTO requestDTO : filteredRequests) {
            LocalDate planningDate = LocalDate.parse(requestDTO.getTeletravailDate());
            
            // Check if a planning already exists for this date and user
            if (planningRepository.existsByUserIdAndPlanningDate(userId, planningDate)) {
                log.info("Planning already exists for user {} on date {}", userId, planningDate);
                continue;
            }
            
            // Create new planning
            TeletravailPlanning planning = new TeletravailPlanning();
            planning.setUserId(userId);
            planning.setUserName(userName);
            planning.setPlanningDate(planningDate);
            planning.setPlanningStatus("PLANNED");
            
            // Set location based on request
            String location = "Domicile";
            if ("non".equalsIgnoreCase(requestDTO.getTravailMaison())) {
                location = requestDTO.getSelectedPays() + " - " + requestDTO.getSelectedGouvernorat();
            }
            planning.setLocation(location);
            
            // Set workType based on the telework request type
            String workType = "Regular";
            if ("exceptionnel".equalsIgnoreCase(requestDTO.getTravailType())) {
                workType = "Exceptional";
            }
            planning.setWorkType(workType);
            
            planning.setReasons(requestDTO.getReason());
            
            plannings.add(planning);
        }
        
        // Save all new plannings
        if (!plannings.isEmpty()) {
            try {
                List<TeletravailPlanning> savedPlannings = planningRepository.saveAll(plannings);
                log.info("Saved {} new planning entries for user {}", savedPlannings.size(), userId);
                
                // Force a flush to ensure data is written to the database immediately
                planningRepository.flush();
                
                return savedPlannings.stream().map(this::convertToDTO).collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Failed to save planning entries to database: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to save planning entries", e);
            }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Update planning for a specific user
     * Used when a new telework request is submitted
     * 
     * @param userId The user ID
     * @param startDate Start date of range
     * @param endDate End date of range
     * @return List of planning response DTOs
     */
    @Transactional
    @CircuitBreaker(name = "teletravailService", fallbackMethod = "updatePlanningForUserFallback")
    public List<PlanningResponseDTO> updatePlanningForUser(Long userId, LocalDate startDate, LocalDate endDate, String authHeader) {
        log.info("Updating planning for user {} from {} to {}", userId, startDate, endDate);
        
        // First, get existing planning entries
        List<TeletravailPlanning> existingPlannings = planningRepository.findByUserIdAndPlanningDateBetween(
                userId, startDate, endDate);
        
        // Then, generate new planning entries
        List<PlanningResponseDTO> newEntries = generatePlanningForUser(userId, startDate, endDate, authHeader);
        
        // Combine existing and new entries
        List<PlanningResponseDTO> allEntries = new ArrayList<>(existingPlannings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
        
        allEntries.addAll(newEntries);
        
        log.info("Updated planning for user {} with {} total entries", userId, allEntries.size());
        return allEntries;
    }
    
    /**
     * Fallback method for circuit breaker when generating planning for user
     */
    public List<PlanningResponseDTO> generatePlanningForUserFallback(Long userId, LocalDate startDate, LocalDate endDate, String authHeader, Exception e) {
        log.error("Failed to generate planning for user {}: {}", userId, e.getMessage());
        return Collections.emptyList();
    }
    
    /**
     * Fallback method for circuit breaker when updating planning for user
     */
    public List<PlanningResponseDTO> updatePlanningForUserFallback(Long userId, LocalDate startDate, LocalDate endDate, String authHeader, Exception e) {
        log.error("Failed to update planning for user {}: {}", userId, e.getMessage());
        return Collections.emptyList();
    }
}
