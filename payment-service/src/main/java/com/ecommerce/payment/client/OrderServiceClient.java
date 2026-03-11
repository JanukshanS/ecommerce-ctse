package com.ecommerce.payment.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class OrderServiceClient {

    private final WebClient webClient;

    public OrderServiceClient(@Value("${order.service.url}") String orderServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(orderServiceUrl)
                .build();
    }

    public void updateOrderPaymentStatus(String orderId, String paymentId, boolean success) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("paymentStatus", success ? "PAID" : "FAILED");
            request.put("paymentId", paymentId);
            if (success) {
                request.put("status", "CONFIRMED");
            }

            webClient.put()
                    .uri("/api/orders/{orderId}/status", orderId)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Order {} status updated after payment {}: success={}", orderId, paymentId, success);
        } catch (Exception e) {
            log.error("Failed to update order {} status after payment: {}", orderId, e.getMessage());
        }
    }
}
