package com.example.planningservice.client;

import com.example.planningservice.dto.TeletravailRequestDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Collections;
import java.util.List;

@FeignClient(name = "teletravail-service", url = "http://localhost:7001", fallback = TeletravailClient.TeletravailClientFallback.class)
public interface TeletravailClient {
    
    @GetMapping("/api/teletravail")
    List<TeletravailRequestDTO> getAllTeletravailRequests();
    
    @GetMapping("/api/teletravail/user/{userId}")
    List<TeletravailRequestDTO> getUserTeletravailRequests(
        @PathVariable("userId") Long userId,
        @RequestHeader("Authorization") String authHeader);

    @Component
    @Slf4j
    class TeletravailClientFallback implements TeletravailClient {
        @Override
        public List<TeletravailRequestDTO> getAllTeletravailRequests() {
            log.error("Circuit breaker triggered: Could not retrieve teletravail requests");
            return Collections.emptyList();
        }

        @Override
        public List<TeletravailRequestDTO> getUserTeletravailRequests(Long userId, String authHeader) {
            log.error("Circuit breaker triggered: Could not retrieve teletravail requests for user {}", userId);
            return Collections.emptyList();
        }
    }
}
