package com.ecommerce.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Translates service-layer exceptions into structured JSON HTTP responses
 * so that unhandled RuntimeExceptions never reach Spring's /error redirect
 * (which would be blocked by Spring Security and return an empty 403).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Duplicate username / email — 409 Conflict */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Catch-all for RuntimeException thrown from AuthService (username exists, etc.) */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage();
        if (msg != null && (msg.contains("already exists") || msg.contains("already registered"))) {
            log.warn("Conflict: {}", msg);
            return buildResponse(HttpStatus.CONFLICT, msg);
        }
        if (msg != null && msg.contains("not found")) {
            log.warn("Not found: {}", msg);
            return buildResponse(HttpStatus.NOT_FOUND, msg);
        }
        log.error("Unexpected error: {}", msg, ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    /** Wrong credentials — 401 */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials");
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    /** Account disabled — 403 */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex) {
        log.warn("Disabled account: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Account is disabled");
    }

    /** Bean Validation failures — 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errors);
        return buildResponse(HttpStatus.BAD_REQUEST, errors);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
