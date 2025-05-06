package com.example.teletravailservice.client;

import com.example.teletravailservice.entity.TeletravailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.LocalDate;

@FeignClient(name = "planning-service", url = "http://localhost:8001", fallbackFactory = PlanningClientFallbackFactory.class)
public interface PlanningClient {

    @PostMapping("/api/planning/update-for-user/{userId}")
    ResponseEntity<String> updatePlanningForUser(
        @PathVariable("userId") Long userId,
        @RequestHeader("Authorization") String authToken
    );
    
    /**
     * Sync a teletravail request with the planning service to ensure planning is updated
     * @param request The teletravail request to sync
     * @return Response from the planning service
     */
    @PostMapping("/api/planning/sync-teletravail")
    ResponseEntity<String> syncTeletravailRequest(
        @RequestBody TeletravailRequest request
    );
}
