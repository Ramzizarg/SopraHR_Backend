package com.example.teletravailservice.config;

import com.example.teletravailservice.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final String jwtSecret;

    public JwtAuthenticationFilter(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("Missing or invalid Authorization header for path: {}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String email = claims.getSubject();
            if (email == null || email.trim().isEmpty()) {
                logger.warn("No subject claim in token");
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "Invalid token: No subject claim");
                return;
            }

            // Set authentication in SecurityContextHolder
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    email, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
            logger.debug("Authenticated user: {}", email);

            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            logger.warn("Token expired: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
        } catch (JwtException e) {
            logger.warn("Invalid token: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Invalid token");
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        ErrorResponse error = new ErrorResponse("Authentication error", message);
        new ObjectMapper().writeValue(response.getWriter(), error);
    }
}