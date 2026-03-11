package com.ecommerce.order.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class PaymentServiceClient {

    private final WebClient webClient;

    public PaymentServiceClient(@Value("${payment.service.url}") String paymentServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .build();
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> processPayment(String orderId, Double amount, String userId) {
        try {
            Map<String, Object> request = Map.of(
                    "orderId", orderId,
                    "amount", amount,
                    "method", "CREDIT_CARD",
                    "description", "Payment for order " + orderId
            );

            Map<String, Object> response = webClient.post()
                    .uri("/api/payments/process")
                    .header("X-User-Id", userId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("Payment processed for order {}: {}", orderId,
                    response != null ? response.get("status") : "null");
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Error calling payment service for order {}: {}", orderId, e.getMessage());
            return Optional.empty();
        }
    }
}
