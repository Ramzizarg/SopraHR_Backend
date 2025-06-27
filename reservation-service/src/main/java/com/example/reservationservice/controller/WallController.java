package com.example.reservationservice.controller;

import com.example.reservationservice.dto.WallDTO;
import com.example.reservationservice.model.Plan;
import com.example.reservationservice.model.Wall;
import com.example.reservationservice.repository.PlanRepository;
import com.example.reservationservice.repository.WallRepository;
import com.example.reservationservice.exception.ResourceNotFoundException;
import com.example.reservationservice.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/walls")
public class WallController {

    private final WallRepository wallRepository;
    private final PlanRepository planRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public WallController(WallRepository wallRepository, 
                        PlanRepository planRepository,
                        JwtTokenProvider jwtTokenProvider) {
        this.wallRepository = wallRepository;
        this.planRepository = planRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping
    public ResponseEntity<List<WallDTO>> getAllWalls() {
        List<WallDTO> walls = wallRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(walls);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WallDTO> getWallById(@PathVariable Long id) {
        Wall wall = wallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wall", "id", id));
        return ResponseEntity.ok(convertToDTO(wall));
    }
    
    @GetMapping("/plan/{planId}")
    public ResponseEntity<List<WallDTO>> getWallsByPlanId(@PathVariable Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));
        
        List<WallDTO> walls = wallRepository.findByPlan(plan).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(walls);
    }

    @PostMapping("/plan/{planId}")
    public ResponseEntity<WallDTO> createWall(
            @PathVariable Long planId,
            @RequestBody WallDTO wallDTO,
            HttpServletRequest request) {
        // Check if user is an admin
        String token = extractToken(request);
        if (!jwtTokenProvider.hasRole(token, "ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));
        
        Wall wall = new Wall();
        wall.setWallId(wallDTO.getWallId() != null ? wallDTO.getWallId() : "wall-" + UUID.randomUUID().toString().substring(0, 8));
        wall.setLeft(wallDTO.getLeft());
        wall.setTop(wallDTO.getTop());
        wall.setWidth(wallDTO.getWidth());
        wall.setHeight(wallDTO.getHeight());
        wall.setPlan(plan);
        
        Wall savedWall = wallRepository.save(wall);
        return new ResponseEntity<>(convertToDTO(savedWall), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WallDTO> updateWall(
            @PathVariable Long id,
            @RequestBody WallDTO wallDTO,
            HttpServletRequest request) {
        // Check if user is an admin
        String token = extractToken(request);
        if (!jwtTokenProvider.hasRole(token, "ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Wall wall = wallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wall", "id", id));
        
        wall.setLeft(wallDTO.getLeft());
        wall.setTop(wallDTO.getTop());
        wall.setWidth(wallDTO.getWidth());
        wall.setHeight(wallDTO.getHeight());
        
        Wall updatedWall = wallRepository.save(wall);
        return ResponseEntity.ok(convertToDTO(updatedWall));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWall(
            @PathVariable Long id,
            HttpServletRequest request) {
        // Check if user is an admin
        String token = extractToken(request);
        if (!jwtTokenProvider.hasRole(token, "ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Wall wall = wallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wall", "id", id));
        
        wallRepository.delete(wall);
        return ResponseEntity.noContent().build();
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    private WallDTO convertToDTO(Wall wall) {
        WallDTO wallDTO = new WallDTO();
        wallDTO.setId(wall.getId());
        wallDTO.setWallId(wall.getWallId());
        wallDTO.setLeft(wall.getLeft());
        wallDTO.setTop(wall.getTop());
        wallDTO.setWidth(wall.getWidth());
        wallDTO.setHeight(wall.getHeight());
        return wallDTO;
    }
}
