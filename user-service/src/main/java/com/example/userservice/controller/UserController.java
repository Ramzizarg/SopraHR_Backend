package com.example.userservice.controller;

import com.example.userservice.dto.UserRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create_user")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest,
                                                   BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = getValidationErrorMessage(bindingResult);
            return ResponseEntity.badRequest()
                    .body(new UserResponse(null, null, null, null, null, null, errorMessage));
        }

        try {
            return userService.createUser(
                    userRequest.email(),
                    userRequest.password(),
                    userRequest.role(),
                    userRequest.firstName(),
                    userRequest.lastName(),
                    userRequest.team()
            ).map(user -> {
                logger.info("User created successfully: {}", user.getEmail());
                return ResponseEntity.ok(new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getTeam().name()
                ));
            }).orElseGet(() -> {
                logger.warn("User creation failed for email: {}", userRequest.email());
                return ResponseEntity.badRequest()
                        .body(new UserResponse(null, null, null, null, null, null,
                                "User creation failed: Email already exists"));
            });
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error during user creation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new UserResponse(null, null, null, null, null, null, e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during user creation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponse(null, null, null, null, null, null,
                            "Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> {
                    logger.info("User retrieved: ID {}", id);
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
                    logger.warn("User not found: ID {}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new UserResponse(null, null, null, null, null, null,
                                    "User not found"));
                });
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers().stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getTeam().name()
                ))
                .collect(Collectors.toList());
        logger.info("Retrieved {} users", users.size());
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UserRequest userRequest,
                                                   BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = getValidationErrorMessage(bindingResult);
            return ResponseEntity.badRequest()
                    .body(new UserResponse(null, null, null, null, null, null, errorMessage));
        }

        try {
            return userService.updateUser(
                    id,
                    userRequest.email(),
                    userRequest.password(),
                    userRequest.role(),
                    userRequest.firstName(),
                    userRequest.lastName(),
                    userRequest.team()
            ).map(user -> {
                logger.info("User updated successfully: ID {}", id);
                return ResponseEntity.ok(new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getTeam().name()
                ));
            }).orElseGet(() -> {
                logger.warn("User not found for update: ID {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new UserResponse(null, null, null, null, null, null,
                                "User not found"));
            });
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error during user update: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new UserResponse(null, null, null, null, null, null, e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during user update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponse(null, null, null, null, null, null,
                            "Internal server error: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<UserResponse> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            logger.info("User deleted successfully: ID {}", id);
            return ResponseEntity.ok(new UserResponse(id, null, null, null, null, null,
                    "User deleted successfully"));
        } else {
            logger.warn("User not found for deletion: ID {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponse(null, null, null, null, null, null,
                            "User not found"));
        }
    }

    private String getValidationErrorMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
    }
}