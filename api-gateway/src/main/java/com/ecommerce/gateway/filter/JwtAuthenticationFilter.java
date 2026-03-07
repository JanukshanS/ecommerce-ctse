package com.ecommerce.gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

/**
 * JWT Authentication Filter.
 * Runs BEFORE routing. Validates the Bearer token and, if valid,
 * forwards the decoded userId and username as headers so downstream
 * services can read them as X-User-Id and X-Username without
 * having to do their own JWT parsing.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // Paths that DO NOT require authentication for ANY HTTP method
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/validate",
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui");

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Skip filter for public endpoints or public GET requests on the catalog
            if (isPublicPath(path)
                    || (request.getMethod().matches("GET") && path.startsWith("/api/catalog/products"))) {
                return chain.filter(exchange);
            }

            // Require Authorization header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = validateAndExtractClaims(token);
                String userId = (String) claims.get("userId");
                String username = claims.getSubject();

                // Mutate the request — add user info headers for downstream services
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId : "")
                        .header("X-Username", username != null ? username : "")
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (ExpiredJwtException e) {
                log.warn("JWT token expired: {}", e.getMessage());
                return onError(exchange, "JWT token has expired", HttpStatus.UNAUTHORIZED);
            } catch (JwtException e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                return onError(exchange, "Invalid JWT token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                log.error("Unexpected error validating JWT: {}", e.getMessage());
                return onError(exchange, "Authentication error: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Claims validateAndExtractClaims(String token) {
        // trim() guards against accidental whitespace in the injected secret
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret.trim());
        Key key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        log.error("Gateway auth error [{}]: {}", status.value(), message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"status\":%d,\"error\":\"Unauthorized\",\"message\":\"%s\"}",
                status.value(), message.replace("\"", "'"));
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // No extra config needed – secret comes from @Value
    }
}
