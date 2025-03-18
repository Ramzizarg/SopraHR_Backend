package com.example.userservice.controller;


import com.example.userservice.dto.*;
import com.example.userservice.service.JwtService;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST})
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                              BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = getValidationErrorMessage(bindingResult);
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null, null, null, errorMessage));
        }

        return userService.authenticate(loginRequest.email(), loginRequest.password())
                .map(user -> {
                    logger.info("Successful login for user: {}", user.getEmail());
                    return ResponseEntity.ok(new AuthResponse(
                            user.getFirstName(),
                            user.getLastName(),
                            user.getRole().name(),
                            jwtService.generateAccessToken(user),
                            jwtService.generateRefreshToken(user)
                    ));
                })
                .orElseGet(() -> {
                    logger.warn("Failed login attempt for email: {}", loginRequest.email());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new AuthResponse(null, null, null, null, null,
                                    "Invalid email or password"));
                });
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest registerRequest,
                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = getValidationErrorMessage(bindingResult);
            return ResponseEntity.badRequest()
                    .body(new UserResponse(null, null, null, null, null, null, errorMessage));
        }

        try {
            return userService.createUser(
                    registerRequest.email(),
                    registerRequest.password(),
                    registerRequest.role(),
                    registerRequest.firstName(),
                    registerRequest.lastName(),
                    registerRequest.team()
            ).map(user -> {
                logger.info("User registered successfully: {}", user.getEmail());
                return ResponseEntity.ok(new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getTeam().name()
                ));
            }).orElseGet(() -> {
                logger.warn("Registration failed for email: {}", registerRequest.email());
                return ResponseEntity.badRequest()
                        .body(new UserResponse(null, null, null, null, null, null,
                                "Registration failed: Email already exists"));
            });
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error during registration: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new UserResponse(null, null, null, null, null, null, e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponse(null, null, null, null, null, null,
                            "Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshRequest refreshRequest,
                                                     BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = getValidationErrorMessage(bindingResult);
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null, null, null, errorMessage));
        }

        if (!jwtService.isTokenValid(refreshRequest.refreshToken())) {
            logger.warn("Invalid refresh token attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, null, null, null, null,
                            "Invalid or expired refresh token"));
        }

        String email = jwtService.getEmailFromToken(refreshRequest.refreshToken());
        return userService.getUserByEmail(email)
                .map(user -> {
                    logger.info("Token refreshed for user: {}", email);
                    return ResponseEntity.ok(new AuthResponse(
                            user.getFirstName(),
                            user.getLastName(),
                            user.getRole().name(),
                            jwtService.generateAccessToken(user),
                            refreshRequest.refreshToken()
                    ));
                })
                .orElseGet(() -> {
                    logger.warn("User not found for token refresh: {}", email);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new AuthResponse(null, null, null, null, null,
                                    "User not found"));
                });
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal String email) {
        if (email == null) {
            logger.warn("Unauthenticated access attempt to /me endpoint");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserResponse(null, null, null, null, null, null,
                            "Authentication required"));
        }

        return userService.getUserByEmail(email)
                .map(user -> {
                    logger.info("User info retrieved for: {}", email);
                    return ResponseEntity.ok(new UserResponse(
                            user.getId(),
                            user.getEmail(),
                            user.getRole().name(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getTeam().name()
                    ));
                })
                .orElseGet(() -> {
                    logger.warn("User not found: {}", email);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new UserResponse(null, null, null, null, null, null,
                                    "User not found"));
                });
    }

    private String getValidationErrorMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
    }
}