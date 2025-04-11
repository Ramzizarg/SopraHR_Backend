package com.example.teletravailservice.controller;

import com.example.teletravailservice.dto.ErrorResponse;
import com.example.teletravailservice.dto.TeletravailRequestDTO;
import com.example.teletravailservice.dto.TeletravailResponseDTO;
import com.example.teletravailservice.entity.TeletravailRequest;
import com.example.teletravailservice.service.TeletravailService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teletravail")
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST})
public class TeletravailController {
    private final TeletravailService teletravailService;

    public TeletravailController(TeletravailService teletravailService) {
        this.teletravailService = teletravailService;
    }

    @PostMapping
    public ResponseEntity<TeletravailResponseDTO> submitTeletravailRequest(
            @Valid @RequestBody TeletravailRequestDTO requestDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal String email) {
        if (bindingResult.hasErrors()) {
            String errorMessage = getValidationErrorMessage(bindingResult);
            log.warn("Validation error: {}", errorMessage);
            return ResponseEntity.badRequest().body(new TeletravailResponseDTO(errorMessage));
        }

        if (email == null) {
            log.warn("Unauthorized request: No authenticated user");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TeletravailResponseDTO("Unauthorized: User not authenticated"));
        }

        try {
            TeletravailRequest savedRequest = teletravailService.saveRequest(requestDTO, email);
            log.info("Teletravail request submitted successfully for user {}: ID {}", email, savedRequest.getId());
            return ResponseEntity.ok(new TeletravailResponseDTO(savedRequest));
        } catch (IllegalArgumentException e) {
            log.warn("Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new TeletravailResponseDTO(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to submit teletravail request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TeletravailResponseDTO("Server error: " + e.getMessage()));
        }
    }

    @GetMapping("/countries")
    public ResponseEntity<?> getCountries() {
        try {
            List<String> countries = teletravailService.getAllCountries();
            log.info("Fetched {} countries successfully", countries.size());
            return ResponseEntity.ok(countries);
        } catch (Exception e) {
            log.error("Failed to fetch countries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error", "Unable to fetch countries: " + e.getMessage()));
        }
    }

    @GetMapping("/regions/{countryName}")
    public ResponseEntity<?> getRegions(@PathVariable String countryName) {
        try {
            if (countryName == null || countryName.trim().isEmpty()) {
                log.warn("Invalid countryName provided: {}", countryName);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid request", "Country name is required"));
            }
            List<String> regions = teletravailService.getRegionsByCountry(countryName);
            log.info("Fetched {} regions for country '{}'", regions.size(), countryName);
            return ResponseEntity.ok(regions);
        } catch (Exception e) {
            log.error("Failed to fetch regions for country '{}': {}", countryName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error", "Unable to fetch regions: " + e.getMessage()));
        }
    }

    private String getValidationErrorMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
    }
}