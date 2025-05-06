package com.example.planningservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String header = request.getHeader("Authorization");
        
        logger.debug("Processing request: {} with auth header: {}", requestURI, header != null ? "present" : "absent");
        
        if (header == null || !header.startsWith("Bearer ")) {
            logger.debug("No Bearer token found in request, continuing filter chain");
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String token = header.substring(7); // Remove "Bearer " prefix
            logger.debug("Attempting to parse JWT token");
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecret.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            String username = claims.getSubject();
            logger.debug("JWT token parsed for user: {}", username);
            
            if (username != null) {
                // Check for different possible claim names for authorities
                Collection<String> authorities = new ArrayList<>();
                
                // Try to get authorities from different claim names
                if (claims.get("authorities") instanceof List) {
                    authorities.addAll((List<String>) claims.get("authorities"));
                    logger.debug("Found authorities in 'authorities' claim");
                } else if (claims.get("roles") instanceof List) {
                    authorities.addAll((List<String>) claims.get("roles"));
                    logger.debug("Found authorities in 'roles' claim");
                } else if (claims.get("role") instanceof String) {
                    authorities.add((String) claims.get("role"));
                    logger.debug("Found authority in 'role' claim: {}", claims.get("role"));
                }
                
                if (!authorities.isEmpty()) {
                    // Map authorities and ensure proper ROLE_ prefix
                    List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                            .map(auth -> {
                                if (!auth.startsWith("ROLE_") && !auth.startsWith("role_")) {
                                    return new SimpleGrantedAuthority("ROLE_" + auth);
                                }
                                return new SimpleGrantedAuthority(auth);
                            })
                            .collect(Collectors.toList());
                    
                    logger.debug("Mapped authorities: {}", grantedAuthorities);
                    
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            grantedAuthorities
                    );
                    
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    logger.debug("Authentication set in SecurityContext");
                } else {
                    logger.warn("No authorities found in JWT token for user: {}", username);
                }
            }
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            logger.error("Could not validate JWT token: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
}
