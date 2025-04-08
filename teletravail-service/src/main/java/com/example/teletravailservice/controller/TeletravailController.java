package com.example.teletravailservice.controller;

import com.example.teletravailservice.dto.TeletravailRequestDTO;
import com.example.teletravailservice.entity.TeletravailRequest;
import com.example.teletravailservice.service.TeletravailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/teletravail")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class TeletravailController {
    private final TeletravailService teletravailService;

    public TeletravailController(TeletravailService teletravailService) {
        this.teletravailService = teletravailService;
    }

    @PostMapping
    public ResponseEntity<?> submitTeletravailRequest(@RequestBody TeletravailRequestDTO requestDTO, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            requestDTO.setUserId(userId);  // Set userId from token
            TeletravailRequest savedRequest = teletravailService.saveRequest(requestDTO, userId);
            log.info("Teletravail request submitted successfully: ID {}", savedRequest.getId());
            return ResponseEntity.ok(savedRequest);
        } catch (IllegalArgumentException e) {
            log.warn("Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Internal server error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save request: " + e.getMessage());
        }
    }

    @GetMapping("/countries")
    public ResponseEntity<List<String>> getCountries() {
        try {
            List<String> countries = teletravailService.getAllCountries();
            log.info("Fetched {} countries", countries.size());
            return ResponseEntity.ok(countries);
        } catch (Exception e) {
            log.error("Error fetching countries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @GetMapping("/regions/{countryName}")
    public ResponseEntity<List<String>> getRegions(@PathVariable String countryName) {
        try {
            List<String> regions = teletravailService.getRegionsByCountry(countryName);
            log.info("Fetched regions for country {}: {}", countryName, regions);
            return ResponseEntity.ok(regions);
        } catch (Exception e) {
            log.error("Error fetching regions for country {}: {}", countryName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }
}