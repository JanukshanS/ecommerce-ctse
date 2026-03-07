package com.ecommerce.gateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Circuit Breaker Fallback Controller.
 * When a downstream service is unavailable and the circuit breaker opens,
 * the gateway routes the request to these fallback endpoints instead of
 * returning a raw 503 to the client.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(buildFallbackBody("auth-service", "Authentication service is temporarily unavailable.")));
    }

    @RequestMapping("/fallback/catalog")
    public Mono<ResponseEntity<Map<String, Object>>> catalogFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(buildFallbackBody("catalog-service", "Product catalog service is temporarily unavailable.")));
    }

    @RequestMapping("/fallback/cart")
    public Mono<ResponseEntity<Map<String, Object>>> cartFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(buildFallbackBody("cart-service", "Cart service is temporarily unavailable.")));
    }

    @RequestMapping("/fallback/order")
    public Mono<ResponseEntity<Map<String, Object>>> orderFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(buildFallbackBody("order-service", "Order service is temporarily unavailable.")));
    }

    @RequestMapping("/fallback/payment")
    public Mono<ResponseEntity<Map<String, Object>>> paymentFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(buildFallbackBody("payment-service", "Payment service is temporarily unavailable. Please try again shortly.")));
    }

    private Map<String, Object> buildFallbackBody(String service, String message) {
        return Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", 503,
            "error", "Service Unavailable",
            "service", service,
            "message", message,
            "circuitBreaker", "OPEN"
        );
    }
}
