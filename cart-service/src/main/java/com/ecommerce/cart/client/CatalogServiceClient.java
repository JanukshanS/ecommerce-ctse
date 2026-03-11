package com.ecommerce.cart.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class CatalogServiceClient {

    private final WebClient webClient;

    public CatalogServiceClient(@Value("${catalog.service.url}") String catalogServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(catalogServiceUrl)
                .build();
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getProduct(String productId) {
        try {
            Map<String, Object> product = webClient.get()
                    .uri("/api/catalog/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return Optional.ofNullable(product);
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Product not found in catalog: {}", productId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error calling catalog service for product {}: {}", productId, e.getMessage());
            throw new RuntimeException("Catalog service unavailable", e);
        }
    }

    public boolean checkStock(String productId, int quantity) {
        try {
            Map<?, ?> response = webClient.get()
                    .uri("/api/catalog/products/{id}/stock-check?quantity={qty}", productId, quantity)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response != null && response.containsKey("available")) {
                return Boolean.TRUE.equals(response.get("available"));
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking stock for product {}: {}", productId, e.getMessage());
            throw new RuntimeException("Catalog service unavailable", e);
        }
    }
}
