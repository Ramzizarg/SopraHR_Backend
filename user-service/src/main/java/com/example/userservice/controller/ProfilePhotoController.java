package com.example.userservice.controller;

import com.example.userservice.service.ProfilePhotoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}, allowedHeaders = "*", allowCredentials = "true")
public class ProfilePhotoController {

    private static final Logger logger = LoggerFactory.getLogger(ProfilePhotoController.class);

    private final ProfilePhotoService profilePhotoService;

    public ProfilePhotoController(ProfilePhotoService profilePhotoService) {
        this.profilePhotoService = profilePhotoService;
    }

    @PostMapping("/{userId}/profile-photo")
    public ResponseEntity<?> uploadProfilePhoto(@PathVariable Long userId,
                                                @RequestParam("file") MultipartFile file) {
        logger.info("Received profile photo upload request for user ID: {}", userId);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload");
        }

        try {
            String photoUrl = profilePhotoService.storeProfilePhoto(userId, file);
            
            Map<String, String> response = new HashMap<>();
            response.put("photoUrl", photoUrl);
            response.put("message", "Profile photo uploaded successfully");
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Failed to upload profile photo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload profile photo: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/profile-photo")
    public ResponseEntity<?> getProfilePhoto(@PathVariable Long userId) {
        logger.info("Retrieving profile photo URL for user ID: {}", userId);

        try {
            String photoUrl = profilePhotoService.getProfilePhotoUrl(userId);
            
            if (photoUrl == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("photoUrl", photoUrl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to retrieve profile photo URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve profile photo: " + e.getMessage());
        }
    }
    
    @GetMapping(value = "/{userId}/profile-photo/{fileName}")
    public ResponseEntity<Resource> serveProfilePhoto(@PathVariable Long userId, @PathVariable String fileName) {
        logger.info("Serving profile photo file: {} for user ID: {}", fileName, userId);
        
        try {
            Resource resource = profilePhotoService.getProfilePhotoFile(userId, fileName);
            
            // Try to determine the content type
            String contentType = MediaType.IMAGE_JPEG_VALUE;  // Default to JPEG
            if (fileName.toLowerCase().endsWith(".png")) {
                contentType = MediaType.IMAGE_PNG_VALUE;
            } else if (fileName.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Failed to serve profile photo file", e);
            return ResponseEntity.notFound().build();
        }
    }
}
