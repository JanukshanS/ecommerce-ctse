package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.model.User;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of("ROLE_USER"))
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user: {}", saved.getUsername());

        String token = jwtUtil.generateToken(saved.getId(), saved.getUsername(), saved.getRoles());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(saved.getId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .roles(saved.getRoles())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoles());
        log.info("User logged in: {}", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();
    }

    public TokenValidationResponse validateToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            return TokenValidationResponse.builder().valid(false).build();
        }
        return TokenValidationResponse.builder()
                .valid(true)
                .userId(jwtUtil.extractUserId(token))
                .username(jwtUtil.extractUsername(token))
                .roles(jwtUtil.extractRoles(token))
                .build();
    }
    public UserInfoResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .build();
    }
}
