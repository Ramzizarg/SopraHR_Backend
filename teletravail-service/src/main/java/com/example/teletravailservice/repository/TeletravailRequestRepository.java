package com.example.teletravailservice.repository;

import com.example.teletravailservice.entity.TeletravailRequest;
import com.example.teletravailservice.entity.TeletravailRequest.TeletravailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeletravailRequestRepository extends JpaRepository<TeletravailRequest, Long> {
    @Query("SELECT r FROM TeletravailRequest r WHERE r.userId = :userId " +
            "AND YEARWEEK(r.teletravailDate, 1) = :yearWeek")
    List<TeletravailRequest> findByUserIdAndWeek(@Param("userId") Long userId,
                                                 @Param("yearWeek") int yearWeek);

    default List<TeletravailRequest> findByUserIdAndWeek(Long userId, int year, int week) {
        return findByUserIdAndWeek(userId, year * 100 + week); // e.g., 202520 for year 2025, week 20
    }

    List<TeletravailRequest> findByUserId(Long userId); // For GET /api/teletravail endpoint
    
    // Find all requests for a specific team
    List<TeletravailRequest> findByTeam(String team);
    
    // Find all requests for a specific team with status
    List<TeletravailRequest> findByTeamAndStatus(String team, TeletravailStatus status);
    
    // Find all requests assigned to a team leader
    List<TeletravailRequest> findByTeamLeaderId(Long teamLeaderId);
    
    // Find all requests for a specific user with status
    List<TeletravailRequest> findByUserIdAndStatus(Long userId, TeletravailStatus status);
    
    // Find all requests regardless of team (for managers)
    List<TeletravailRequest> findAllByOrderByCreatedAtDesc();
    
    // Find requests by user ID and specific date
    // This is used when deleting associated teletravail requests when planning entries are deleted
    List<TeletravailRequest> findByUserIdAndTeletravailDate(Long userId, String teletravailDate);
    
    // Find requests by user ID and date range (for planning page)
    @Query("SELECT r FROM TeletravailRequest r WHERE r.userId = :userId " +
            "AND r.teletravailDate >= :startDate AND r.teletravailDate <= :endDate " +
            "ORDER BY r.teletravailDate ASC")
    List<TeletravailRequest> findByUserIdAndTeletravailDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);
    
    // Find requests by team and date range (for team planning view)
    @Query("SELECT r FROM TeletravailRequest r WHERE r.team = :team " +
            "AND r.teletravailDate >= :startDate AND r.teletravailDate <= :endDate " +
            "ORDER BY r.teletravailDate ASC")
    List<TeletravailRequest> findByTeamAndTeletravailDateBetween(
            @Param("team") String team,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);
            
    // Find all requests within a date range (for manager planning view)
    @Query("SELECT r FROM TeletravailRequest r WHERE " +
            "r.teletravailDate >= :startDate AND r.teletravailDate <= :endDate " +
            "ORDER BY r.team ASC, r.teletravailDate ASC")
    List<TeletravailRequest> findByTeletravailDateBetween(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);
}