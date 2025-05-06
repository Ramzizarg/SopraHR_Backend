package com.example.teletravailservice.client;

import com.example.teletravailservice.entity.TeletravailRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PlanningClientFallbackFactory implements FallbackFactory<PlanningClient> {

    @Override
    public PlanningClient create(Throwable cause) {
        return new PlanningClient() {
            @Override
            public ResponseEntity<String> updatePlanningForUser(Long userId, String authToken) {
                log.error("Unable to update planning for user {}: {}", userId, cause.getMessage());
                // We don't want to fail the telework request just because planning couldn't be updated
                return ResponseEntity.ok("Planning service unavailable, planning will be updated later");
            }
            
            @Override
            public ResponseEntity<String> syncTeletravailRequest(TeletravailRequest request) {
                log.error("Unable to sync teletravail request {} with planning service: {}", 
                          request.getId(), cause.getMessage());
                // We don't want to fail the telework request just because planning couldn't be updated
                return ResponseEntity.ok("Planning service unavailable, planning will be updated later");
            }
        };
    }
}
