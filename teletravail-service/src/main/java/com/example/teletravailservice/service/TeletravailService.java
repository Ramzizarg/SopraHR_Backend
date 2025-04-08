package com.example.teletravailservice.service;


import com.example.teletravailservice.dto.TeletravailRequestDTO;
import com.example.teletravailservice.entity.TeletravailRequest;
import com.example.teletravailservice.repository.TeletravailRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${user.service.url}")
    private String userServiceUrl;

    private static final String COUNTRY_STATE_CITY_API = "https://api.countrystatecity.in/v1";
    private static final String API_KEY = "YOUR_API_KEY_HERE"; // Replace with your CountryStateCity API key

    public TeletravailService(TeletravailRequestRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    public TeletravailRequest saveRequest(TeletravailRequestDTO dto, Long userId) {
        if (dto.getTravailType() == null || dto.getTeletravailDate() == null || dto.getTravailMaison() == null) {
            log.warn("Validation failed: Required fields are missing");
            throw new IllegalArgumentException("Required fields (travailType, teletravailDate, travailMaison) cannot be null");
        }

        // Validate user exists in user-service
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> userResponse = restTemplate.exchange(
                userServiceUrl + "/validate/" + userId,
                HttpMethod.GET,
                entity,
                Map.class
        );
        if (!userResponse.getStatusCode().is2xxSuccessful() || userResponse.getBody() == null) {
            log.warn("User not found: {}", userId);
            throw new IllegalArgumentException("User not found: " + userId);
        }

        boolean requiresLocation = "non".equalsIgnoreCase(dto.getTravailMaison());
        boolean requiresReason = !"reguliere".equalsIgnoreCase(dto.getTravailType());

        if (requiresLocation && (dto.getSelectedPays() == null || dto.getSelectedGouvernorat() == null)) {
            log.warn("Validation failed: Location fields are required when travailMaison is 'non'");
            throw new IllegalArgumentException("Pays and Gouvernorat are required when working outside home");
        }

        if (requiresReason && (dto.getReason() == null || dto.getReason().trim().isEmpty())) {
            log.warn("Validation failed: Reason is required for non-regular teletravail");
            throw new IllegalArgumentException("Reason is required for non-regular teletravail");
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
        log.info("Teletravail request saved successfully: ID {}", savedRequest.getId());
        return savedRequest;
    }

    /**
     * Fetches all countries from the CountryStateCity API.
     * @return List of country names
     */
    public List<String> getAllCountries() {
        String url = COUNTRY_STATE_CITY_API + "/countries";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-CSCAPI-KEY", API_KEY);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        if (response.getBody() != null) {
            return response.getBody().stream()
                    .map(country -> (String) country.get("name"))
                    .collect(Collectors.toList());
        }
        log.warn("No countries fetched from API");
        return Collections.emptyList();
    }

    /**
     * Fetches regions/gouvernorats for a given country from the CountryStateCity API.
     * @param countryName Name of the country (e.g., "France")
     * @return List of region names
     */
    public List<String> getRegionsByCountry(String countryName) {
        String countriesUrl = COUNTRY_STATE_CITY_API + "/countries";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-CSCAPI-KEY", API_KEY);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Fetch all countries to find the ISO2 code
        ResponseEntity<List<Map<String, Object>>> countriesResponse = restTemplate.exchange(
                countriesUrl,
                HttpMethod.GET,
                entity,
                new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        if (countriesResponse.getBody() == null) {
            log.warn("Failed to fetch countries for region lookup");
            return Collections.emptyList();
        }

        String countryIso2 = countriesResponse.getBody().stream()
                .filter(c -> countryName.equalsIgnoreCase((String) c.get("name")))
                .map(c -> (String) c.get("iso2"))
                .findFirst()
                .orElse(null);

        if (countryIso2 == null) {
            log.warn("Country not found: {}", countryName);
            return List.of("Country not found: " + countryName);
        }

        // Fetch regions for the country
        String regionsUrl = COUNTRY_STATE_CITY_API + "/countries/" + countryIso2 + "/states";
        ResponseEntity<List<Map<String, Object>>> regionsResponse = restTemplate.exchange(
                regionsUrl,
                HttpMethod.GET,
                entity,
                new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        if (regionsResponse.getBody() != null) {
            return regionsResponse.getBody().stream()
                    .map(region -> (String) region.get("name"))
                    .collect(Collectors.toList());
        }
        log.warn("No regions found for country: {}", countryName);
        return List.of("No regions available for " + countryName);
    }
}