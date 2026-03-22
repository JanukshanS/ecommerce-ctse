package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, and token validation")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate a JWT token (used internally by other services via API Gateway)")
    public ResponseEntity<TokenValidationResponse> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(TokenValidationResponse.builder().valid(false).build());
        }
        String token = authHeader.substring(7);
        return ResponseEntity.ok(authService.validateToken(token));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "auth-service"));
    }

    /**
     * Internal endpoint for inter-service communication.
     * Called by catalog-service to fetch seller information when creating a product.
     * This endpoint is NOT exposed to the public internet (internal ingress only).
     */
    @GetMapping("/internal/users/{userId}")
    @Operation(summary = "Get user info by ID (internal - called by other microservices)")
    public ResponseEntity<UserInfoResponse> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }
}
