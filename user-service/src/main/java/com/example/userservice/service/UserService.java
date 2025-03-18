package com.example.userservice.service;

import com.example.userservice.entity.Role;
import com.example.userservice.entity.Team;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // CREATE
    public Optional<User> createUser(String email, String password, String role,
                                     String firstName, String lastName, String team) {
        validateRequiredFields(email, password, role, firstName, lastName, team);

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            logger.warn("Create attempt with existing email: {}", normalizedEmail);
            return Optional.empty();
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.fromString(role));
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setTeam(Team.fromString(team));

        User savedUser = userRepository.save(user);
        logger.info("User created successfully: {}", normalizedEmail);
        return Optional.of(savedUser);
    }

    // READ (Single User by ID)
    public Optional<User> getUserById(Long id) {
        if (id == null) {
            logger.warn("Attempt to get user with null ID");
            return Optional.empty();
        }

        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            logger.debug("User found by ID: {}", id);
        } else {
            logger.debug("User not found by ID: {}", id);
        }
        return user;
    }

    // READ (Single User by Email)
    public Optional<User> getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Attempt to get user with null or empty email");
            return Optional.empty();
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> user = userRepository.findByEmail(normalizedEmail);
        if (user.isPresent()) {
            logger.debug("User found: {}", normalizedEmail);
        } else {
            logger.debug("User not found: {}", normalizedEmail);
        }
        return user;
    }

    // READ (All Users)
    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        logger.debug("Retrieved {} users", users.size());
        return users;
    }

    // UPDATE
    public Optional<User> updateUser(Long id, String email, String password, String role,
                                     String firstName, String lastName, String team) {
        if (id == null) {
            logger.warn("Attempt to update user with null ID");
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for update: ID {}", id);
            return Optional.empty();
        }

        User user = userOpt.get();

        if (email != null && !email.trim().isEmpty()) {
            String normalizedEmail = email.trim().toLowerCase();
            if (!normalizedEmail.equals(user.getEmail()) &&
                    userRepository.findByEmail(normalizedEmail).isPresent()) {
                throw new IllegalArgumentException("Email already in use by another user");
            }
            user.setEmail(normalizedEmail);
        }

        if (password != null && !password.isEmpty()) {
            if (password.length() < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters");
            }
            user.setPassword(passwordEncoder.encode(password));
        }

        if (role != null && !role.trim().isEmpty()) {
            user.setRole(Role.fromString(role));
        }

        if (firstName != null && !firstName.trim().isEmpty()) {
            user.setFirstName(firstName.trim());
        }

        if (lastName != null && !lastName.trim().isEmpty()) {
            user.setLastName(lastName.trim());
        }

        if (team != null && !team.trim().isEmpty()) {
            user.setTeam(Team.fromString(team));
        }

        User updatedUser = userRepository.save(user);
        logger.info("User updated successfully: {}", updatedUser.getEmail());
        return Optional.of(updatedUser);
    }

    // DELETE
    public boolean deleteUser(Long id) {
        if (id == null) {
            logger.warn("Attempt to delete user with null ID");
            return false;
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for deletion: ID {}", id);
            return false;
        }

        userRepository.deleteById(id);
        logger.info("User deleted successfully: ID {}", id);
        return true;
    }

    // Authentication (from previous version)
    public Optional<User> authenticate(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Email and password must not be empty");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            logger.info("Successful authentication for user: {}", normalizedEmail);
            return userOpt;
        }

        logger.warn("Failed authentication attempt for email: {}", normalizedEmail);
        return Optional.empty();
    }

    private void validateRequiredFields(String email, String password, String role,
                                        String firstName, String lastName, String team) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role is required");
        }
        Role.fromString(role);
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (team == null || team.trim().isEmpty()) {
            throw new IllegalArgumentException("Team is required");
        }
        Team.fromString(team);
    }
}