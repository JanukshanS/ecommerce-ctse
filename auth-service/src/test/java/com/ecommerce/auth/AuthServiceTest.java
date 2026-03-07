package com.ecommerce.auth;

import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.model.User;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.security.JwtUtil;
import com.ecommerce.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
    }

    @Test
    void register_ShouldCreateUserAndReturnToken() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .id("user1")
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .roles(Set.of("ROLE_USER"))
                .enabled(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken("user1", "testuser", Set.of("ROLE_USER")))
                .thenReturn("mock.jwt.token");

        var response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenUsernameExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsValid() {
        User user = User.builder()
                .id("user1")
                .username("testuser")
                .email("test@example.com")
                .roles(Set.of("ROLE_USER"))
                .enabled(true)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("user1", "testuser", Set.of("ROLE_USER")))
                .thenReturn("mock.jwt.token");

        var response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
    }

    @Test
    void validateToken_ShouldReturnValid_WhenTokenIsLegitimate() {
        when(jwtUtil.validateToken("validtoken")).thenReturn(true);
        when(jwtUtil.extractUserId("validtoken")).thenReturn("user1");
        when(jwtUtil.extractUsername("validtoken")).thenReturn("testuser");
        when(jwtUtil.extractRoles("validtoken")).thenReturn(Set.of("ROLE_USER"));

        var response = authService.validateToken("validtoken");

        assertTrue(response.isValid());
        assertEquals("testuser", response.getUsername());
    }

    @Test
    void validateToken_ShouldReturnInvalid_WhenTokenIsExpired() {
        when(jwtUtil.validateToken("expiredtoken")).thenReturn(false);

        var response = authService.validateToken("expiredtoken");

        assertFalse(response.isValid());
    }
}
