package com.example.userservice.service;

import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ProfilePhotoService {

    private static final Logger logger = LoggerFactory.getLogger(ProfilePhotoService.class);
    
    @Value("${file.upload-dir:uploads/profile-photos}")
    private String uploadDir;
    
    @Value("${app.base-url:http://localhost:9001}")
    private String baseUrl;
    
    private final UserRepository userRepository;
    
    public ProfilePhotoService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public String storeProfilePhoto(Long userId, MultipartFile file) throws IOException {
        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Create the upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("Created directory: {}", uploadPath);
        }
        
        // Get file extension
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        // Generate a unique file name
        String newFileName = userId + "_" + UUID.randomUUID() + fileExtension;
        Path filePath = uploadPath.resolve(newFileName);
        
        // Save the file to the file system
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Stored file: {}", filePath);
        
        // Store only the filename in the database
        user.setProfilePhotoUrl(newFileName);
        userRepository.save(user);
        logger.info("Updated profile photo filename for user: {}", userId);
        
        // For API response, return the full URL
        return baseUrl + "/api/users/" + userId + "/profile-photo/" + newFileName;
    }
    
    public String getProfilePhotoUrl(Long userId) {
        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        String filename = user.getProfilePhotoUrl();
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        
        // Construct the full URL from the stored filename
        return baseUrl + "/api/users/" + userId + "/profile-photo/" + filename;
    }
    
    // Method to serve the actual image file
    public Resource getProfilePhotoFile(Long userId, String filename) {
        try {
            // Find the user to validate that the requested filename matches what's stored
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            
            // Check if the requested filename matches the one stored for this user
            if (!filename.equals(user.getProfilePhotoUrl())) {
                logger.warn("Requested filename {} does not match stored filename {} for user {}", 
                           filename, user.getProfilePhotoUrl(), userId);
                // Still proceed with the file lookup as we're just using the filename parameter
            }
            
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            UrlResource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found: " + filename, ex);
        }
    }
}
