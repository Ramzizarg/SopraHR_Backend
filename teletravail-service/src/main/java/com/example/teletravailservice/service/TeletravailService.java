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

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private static final int MAX_DAYS_PER_WEEK = 2;

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
            log.warn("User validation failed for email {}: {}", email, e.getMessage());
            throw e;
        }

        LocalDate requestDate = LocalDate.parse(dto.getTeletravailDate());
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int requestWeek = requestDate.get(weekFields.weekOfWeekBasedYear());
        int requestYear = requestDate.get(weekFields.weekBasedYear());

        List<TeletravailRequest> existingRequests = repository.findByUserIdAndWeek(userId, requestYear, requestWeek);

        if (existingRequests.size() >= MAX_DAYS_PER_WEEK) {
            log.warn("User {} exceeded max days limit for week {}-{}", email, requestYear, requestWeek);
            throw new IllegalArgumentException("Vous avez déjà soumis des demandes pour 2 jours cette semaine.");
        }

        if (existingRequests.stream().anyMatch(r -> LocalDate.parse(r.getTeletravailDate()).equals(requestDate))) {
            log.warn("User {} attempted duplicate request for date {}", email, requestDate);
            throw new IllegalArgumentException("Vous avez déjà une demande pour ce jour.");
        }

        for (TeletravailRequest req : existingRequests) {
            LocalDate existingDate = LocalDate.parse(req.getTeletravailDate());
            long dayDifference = Math.abs(existingDate.toEpochDay() - requestDate.toEpochDay());
            if (dayDifference == 1) {
                log.warn("User {} attempted consecutive days: {} and {}", email, existingDate, requestDate);
                throw new IllegalArgumentException("Les jours de télétravail ne doivent pas être consécutifs.");
            }
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
        log.error("User service failed for email {}: {}", email, t.getMessage());
        throw new IllegalStateException("Service utilisateur indisponible, veuillez réessayer plus tard.");
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
            log.info("Fetched {} countries successfully", countries.size());
            return countries;
        }

        log.warn("No countries fetched from API");
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "countryApi", fallbackMethod = "countryApiFallback")
    public List<String> getRegionsByCountry(String countryName) {
        if (countryName == null || countryName.trim().isEmpty()) {
            log.warn("Invalid countryName provided: {}", countryName);
            throw new IllegalArgumentException("Le nom du pays est requis.");
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
            throw new IllegalStateException("Impossible de récupérer les pays.");
        }

        String countryIso2 = countriesResponse.getBody().stream()
                .filter(c -> countryName.equalsIgnoreCase((String) c.get("name")))
                .map(c -> (String) c.get("iso2"))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Country not found: {}", countryName);
                    return new IllegalArgumentException("Pays non trouvé : " + countryName);
                });

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
        return Collections.singletonList("Service de localisation indisponible");
    }

    /**
     * Retrieves all teletravail requests for a given user.
     * @param userId ID of the user.
     * @return List of TeletravailRequest entities.
     */
    public List<TeletravailRequest> getUserRequests(Long userId) {
        List<TeletravailRequest> requests = repository.findByUserId(userId);
        log.info("Fetched {} teletravail requests for user ID {}", requests.size(), userId);
        return requests;
    }

    private void validateRequest(TeletravailRequestDTO dto) {
        boolean requiresLocation = "non".equalsIgnoreCase(dto.getTravailMaison());
        boolean requiresReason = !"reguliere".equalsIgnoreCase(dto.getTravailType());

        if (dto.getTeletravailDate() == null || dto.getTeletravailDate().trim().isEmpty()) {
            log.warn("Teletravail date is missing");
            throw new IllegalArgumentException("La date de télétravail est requise.");
        }

        if (dto.getTravailType() == null || dto.getTravailType().trim().isEmpty()) {
            log.warn("Travail type is missing");
            throw new IllegalArgumentException("Le type de travail est requis.");
        }

        if (dto.getTravailMaison() == null || dto.getTravailMaison().trim().isEmpty()) {
            log.warn("Travail maison is missing");
            throw new IllegalArgumentException("Le champ 'Travail à domicile' est requis.");
        }

        if (requiresLocation && (dto.getSelectedPays() == null || dto.getSelectedPays().trim().isEmpty() ||
                dto.getSelectedGouvernorat() == null || dto.getSelectedGouvernorat().trim().isEmpty())) {
            log.warn("Location fields are missing for travailMaison='non'");
            throw new IllegalArgumentException("Le pays et la région sont requis lorsque vous ne travaillez pas à domicile.");
        }

        if (requiresReason && (dto.getReason() == null || dto.getReason().trim().isEmpty())) {
            log.warn("Reason is missing for non-regular travailType");
            throw new IllegalArgumentException("Une raison est requise pour un télétravail non régulier.");
        }

        LocalDate date = LocalDate.parse(dto.getTeletravailDate());
        if (date.getDayOfWeek().getValue() > 5) {
            log.warn("Requested date {} is a weekend", dto.getTeletravailDate());
            throw new IllegalArgumentException("Les weekends ne sont pas disponibles pour le télétravail.");
        }
    }

    public Long getUserIdByEmail(String email) {
        return userClient.validateUserByEmail(email);
    }
}