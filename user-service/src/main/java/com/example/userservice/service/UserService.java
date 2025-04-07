package com.example.userservice.service;

import com.example.userservice.entity.Role;
import com.example.userservice.entity.Team;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.reset-password.url}")
    private String resetPasswordUrl;

    @Value("${app.company.name}")
    private String companyName;

    @Value("${app.company.email}")
    private String companyEmail;

    @Value("${app.company.logo.url}")
    private String companyLogoUrl;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    public Optional<User> createUser(String email, String password, String role,
                                     String firstName, String lastName, String team) {
        try {
            validateRequiredFields(email, password, role, firstName, lastName, team);

            String normalizedEmail = email.trim().toLowerCase();
            if (userRepository.findByEmail(normalizedEmail).isPresent()) {
                logger.warn("Create attempt with existing email: {}", normalizedEmail);
                return Optional.empty();
            }

            User user = new User();
            user.setEmail(normalizedEmail);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(Role.fromString(role)); // Now works with "employee"
            user.setFirstName(firstName.trim());
            user.setLastName(lastName.trim());
            user.setTeam(Team.fromString(team));

            User savedUser = userRepository.save(user);
            logger.info("User created successfully: {}", normalizedEmail);
            return Optional.of(savedUser);
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error during user creation: {}", e.getMessage());
            throw e; // Re-throw to be caught by controller
        } catch (Exception e) {
            logger.error("Unexpected error during user creation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

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

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        logger.debug("Retrieved {} users", users.size());
        return users;
    }

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

    public String generateResetToken(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            logger.warn("No user found for password reset: {}", normalizedEmail);
            return null;
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        user.setResetToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        logger.info("Reset token generated for user: {}", normalizedEmail);
        try {
            sendResetEmail(normalizedEmail, token);
        } catch (RuntimeException e) {
            logger.warn("Failed to send reset email for user: {}. Token generated but email not sent.", normalizedEmail);
            throw e;
        }
        return token;
    }

    public Optional<User> resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Reset token is required");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }

        Optional<User> userOpt = userRepository.findByResetToken(token);
        if (userOpt.isEmpty()) {
            logger.warn("Invalid reset token: {}", token);
            return Optional.empty();
        }

        User user = userOpt.get();
        if (user.getTokenExpiry() == null || user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            logger.warn("Expired reset token: {}", token);
            user.setResetToken(null);
            user.setTokenExpiry(null);
            userRepository.save(user);
            return Optional.empty();
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setTokenExpiry(null);
        User updatedUser = userRepository.save(user);

        logger.info("Password reset successfully for user: {}", user.getEmail());
        return Optional.of(updatedUser);
    }

    private void sendResetEmail(String email, String token) {
        if (companyName == null || companyEmail == null || resetPasswordUrl == null || companyLogoUrl == null) {
            throw new IllegalStateException("Email configuration properties are not properly set");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Password Reset Request - " + companyName);
            helper.setFrom(companyEmail);

            String resetLink = resetPasswordUrl + "?token=" + token;
            String htmlContent = String.format("""  
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <style>
            body { font-family: Arial, sans-serif; color: #333; font-size: 20px; margin: 0; padding: 0; }
            .container { max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px; }
            .header { text-align: center; }
            .header img { max-width: 100px; }
            .content { margin: 20px 0; text-align: center; }
            .button {
                display: inline-block;
                padding: 14px 28px;
                color: #ffffff;
                background: linear-gradient(135deg, #f67200 0%%, #de1823 100%%);
                text-decoration: none;
                border-radius: 50px;
                box-shadow: 0 4px 15px rgba(222, 24, 35, 0.4);
                font-weight: 600;
                text-transform: uppercase;
                letter-spacing: 1px;
                transition: all 0.3s ease;
            }
            .button:hover {
                background: linear-gradient(135deg, #de1823 0%%, #f67200 100%%);
                box-shadow: 0 6px 20px rgba(222, 24, 35, 0.6);
                transform: scale(1.05) translateY(-2px);
                color: #ffffff;
            }
            .footer { text-align: center; font-size: 12px; color: #777; margin-top: 20px; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <img src="%s" alt="%s Logo">
                <h2>Password Reset Request</h2>
            </div>
            <div class="content">
                <p>Dear User,</p>
                <p>We received a request to reset your password. Click the button below to reset it:</p>
                <!-- Button with Inline Styles for Compatibility -->
                <p><a href="%s" class="button" role="button" aria-label="Reset your password" style="color: #ffffff; background: linear-gradient(135deg, #f67200 0%%, #de1823 100%%); padding: 14px 28px; text-decoration: none; border-radius: 50px; box-shadow: 0 4px 15px rgba(222, 24, 35, 0.4); font-weight: 600; text-transform: uppercase; letter-spacing: 1px;">Reset Password</a></p>
                <p>This link will expire in 1 hour. If you did not request this, please ignore this email or contact our support team.</p>
            </div>
            <div class="footer">
                <p>Regards,<br>%s Team</p>
                <p>© %d %s. All rights reserved.</p>
                <p><a href="mailto:%s" aria-label="Contact %s Support">Contact Support</a></p>
            </div>
        </div>
    </body>
    </html>
    """,
                    companyLogoUrl, companyName, resetLink, companyName, LocalDateTime.now().getYear(), companyName, companyEmail, companyName);
            helper.setText(htmlContent, true);


            mailSender.send(message);
            logger.info("Reset email sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send reset email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send reset email: " + e.getMessage(), e);
        }
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
        Role.fromString(role); // Will throw if invalid
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (team == null || team.trim().isEmpty()) {
            throw new IllegalArgumentException("Team is required");
        }
        Team.fromString(team); // Will throw if invalid
    }
}