package com.example.teletravailservice.service;

import com.example.teletravailservice.client.UserClient;
import com.example.teletravailservice.dto.TeletravailRequestDTO;
import com.example.teletravailservice.entity.TeletravailRequest;
import com.example.teletravailservice.repository.TeletravailRequestRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class TeletravailService {
    private final TeletravailRequestRepository repository;
    private final RestTemplate restTemplate;
    private final UserClient userClient;

    @Value("${api.countrystatecity.key}")
    private String apiKey;

    private static final String COUNTRY_STATE_CITY_API = "https://api.countrystatecity.in/v1";

    public TeletravailService(TeletravailRequestRepository repository, RestTemplate restTemplate, UserClient userClient) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.userClient = userClient;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "userServiceFallback")
    public TeletravailRequest saveRequest(TeletravailRequestDTO dto, String email) {
        validateRequest(dto);

        Long userId;
        try {
            userId = userClient.validateUserByEmail(email);
        } catch (IllegalArgumentException e) {
            log.warn("User validation failed: {}", e.getMessage());
            throw e;
        }

        TeletravailRequest request = new TeletravailRequest();
        request.setUserId(userId);
        request.setTravailType(dto.getTravailType());
        request.setTeletravailDate(dto.getTeletravailDate());
        request.setTravailMaison(dto.getTravailMaison());
        request.setSelectedPays(dto.getSelectedPays());
        request.setSelectedGouvernorat(dto.getSelectedGouvernorat());
        request.setReason(dto.getReason());

        TeletravailRequest savedRequest = repository.save(request);
        log.info("Teletravail request saved successfully for user {}: ID {}", email, savedRequest.getId());
        return savedRequest;
    }

    public TeletravailRequest userServiceFallback(TeletravailRequestDTO dto, String email, Throwable t) {
        log.error("User service failed for email: {}, error: {}", email, t.getMessage());
        throw new IllegalStateException("User service unavailable, please try again later");
    }

    @CircuitBreaker(name = "countryApi", fallbackMethod = "countryApiFallback")
    public List<String> getAllCountries() {
        String url = COUNTRY_STATE_CITY_API + "/countries";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-CSCAPI-KEY", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        if (response.getBody() != null) {
            List<String> countries = response.getBody().stream()
                    .map(country -> (String) country.get("name"))
                    .sorted()
                    .collect(Collectors.toList());
            log.info("Fetched {} countries", countries.size());
            return countries;
        }
        log.warn("No countries fetched from API");
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "countryApi", fallbackMethod = "countryApiFallback")
    public List<String> getRegionsByCountry(String countryName) {
        if (countryName == null || countryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Country name is required");
        }

        String countriesUrl = COUNTRY_STATE_CITY_API + "/countries";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-CSCAPI-KEY", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Map<String, Object>>> countriesResponse = restTemplate.exchange(
                countriesUrl,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        if (countriesResponse.getBody() == null) {
            log.warn("Failed to fetch countries for region lookup");
            throw new IllegalStateException("Unable to fetch countries");
        }

        String countryIso2 = countriesResponse.getBody().stream()
                .filter(c -> countryName.equalsIgnoreCase((String) c.get("name")))
                .map(c -> (String) c.get("iso2"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Country not found: " + countryName));

        String regionsUrl = COUNTRY_STATE_CITY_API + "/countries/" + countryIso2 + "/states";
        ResponseEntity<List<Map<String, Object>>> regionsResponse = restTemplate.exchange(
                regionsUrl,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        if (regionsResponse.getBody() != null) {
            List<String> regions = regionsResponse.getBody().stream()
                    .map(region -> (String) region.get("name"))
                    .sorted()
                    .collect(Collectors.toList());
            log.info("Fetched {} regions for country '{}'", regions.size(), countryName);
            return regions;
        }
        log.warn("No regions found for country: {}", countryName);
        return Collections.emptyList();
    }

    public List<String> countryApiFallback(Throwable t) {
        log.error("Country API failed: {}", t.getMessage());
        return List.of("Location service unavailable");
    }

    private void validateRequest(TeletravailRequestDTO dto) {
        boolean requiresLocation = "non".equalsIgnoreCase(dto.getTravailMaison());
        boolean requiresReason = !"reguliere".equalsIgnoreCase(dto.getTravailType());

        if (requiresLocation && (dto.getSelectedPays() == null || dto.getSelectedPays().trim().isEmpty() ||
                dto.getSelectedGouvernorat() == null || dto.getSelectedGouvernorat().trim().isEmpty())) {
            throw new IllegalArgumentException("Country and region are required when working outside home");
        }

        if (requiresReason && (dto.getReason() == null || dto.getReason().trim().isEmpty())) {
            throw new IllegalArgumentException("Reason is required for non-regular teletravail");
        }

        if (dto.getTeletravailDate() == null || dto.getTeletravailDate().trim().isEmpty()) {
            throw new IllegalArgumentException("Teletravail date is required");
        }

        if (dto.getTravailType() == null || dto.getTravailType().trim().isEmpty()) {
            throw new IllegalArgumentException("Travail type is required");
        }
    }
}