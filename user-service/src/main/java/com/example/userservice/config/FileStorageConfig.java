package com.example.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads/profile-photos}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Map the physical path of uploaded files to a URL path
        Path uploadPath = Paths.get(uploadDir);
        String resourcePath = uploadPath.toFile().getAbsolutePath();
        
        // This will map /uploads/** to the physical directory where files are stored
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + resourcePath + "/");
    }
}
