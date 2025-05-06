package com.example.planningservice.repository;

import com.example.planningservice.entity.TeletravailPlanning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeletravailPlanningRepository extends JpaRepository<TeletravailPlanning, Long> {
    
    List<TeletravailPlanning> findByUserId(Long userId);
    
    @Query("SELECT tp FROM TeletravailPlanning tp WHERE tp.planningDate >= :startDate AND tp.planningDate <= :endDate")
    List<TeletravailPlanning> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT tp FROM TeletravailPlanning tp WHERE tp.userId = :userId AND tp.planningDate >= :startDate AND tp.planningDate <= :endDate")
    List<TeletravailPlanning> findByUserIdAndDateRange(
            @Param("userId") Long userId, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(tp) FROM TeletravailPlanning tp WHERE tp.userId = :userId AND YEAR(tp.planningDate) = :year AND MONTH(tp.planningDate) = :month")
    int countTeletravailDaysByUserAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);
    
    boolean existsByUserIdAndPlanningDate(Long userId, LocalDate planningDate);
    
    /**
     * Find a planning entry for a specific user on a specific date
     * 
     * @param userId User ID to find
     * @param planningDate Planning date to find
     * @return Optional containing the planning entry if found
     */
    Optional<TeletravailPlanning> findByUserIdAndPlanningDate(Long userId, LocalDate planningDate);
    
    /**
     * Check if any planning entries exist for a user within a date range
     * 
     * @param userId User ID to check
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return true if any planning entries exist, false otherwise
     */
    boolean existsByUserIdAndPlanningDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Find all planning entries for a user within a date range
     * 
     * @param userId User ID to search for
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of planning entries
     */
    List<TeletravailPlanning> findByUserIdAndPlanningDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
