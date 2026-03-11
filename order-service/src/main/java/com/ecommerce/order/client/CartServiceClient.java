package com.ecommerce.order.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class CartServiceClient {

    private final WebClient webClient;

    public CartServiceClient(@Value("${cart.service.url}") String cartServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(cartServiceUrl)
                .build();
    }

    public void clearCart(String userId) {
        try {
            webClient.delete()
                    .uri("/api/cart")
                    .header("X-User-Id", userId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Cart cleared for user {} after order creation", userId);
        } catch (Exception e) {
            log.warn("Failed to clear cart for user {}: {}", userId, e.getMessage());
        }
    }
}
