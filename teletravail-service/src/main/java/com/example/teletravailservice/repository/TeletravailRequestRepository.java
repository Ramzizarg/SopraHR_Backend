package com.example.teletravailservice.repository;

import com.example.teletravailservice.entity.TeletravailRequest;
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
}