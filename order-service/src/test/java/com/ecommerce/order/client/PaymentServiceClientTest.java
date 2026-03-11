package com.ecommerce.order.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceClientTest {

    private MockWebServer mockWebServer;
    private PaymentServiceClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        client = new PaymentServiceClient(baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void processPayment_shouldReturnResponse_whenSuccessful() {
        String paymentJson = """
                {"id":"pay-1","orderId":"order-1","status":"COMPLETED","amount":99.99}
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody(paymentJson)
                .addHeader("Content-Type", "application/json"));

        Optional<Map<String, Object>> result = client.processPayment("order-1", 99.99, "user-123");

        assertTrue(result.isPresent());
        assertEquals("pay-1", result.get().get("id"));
        assertEquals("order-1", result.get().get("orderId"));
        assertEquals("COMPLETED", result.get().get("status"));
        assertEquals(99.99, ((Number) result.get().get("amount")).doubleValue());
    }

    @Test
    void processPayment_shouldReturnEmpty_whenServiceFails() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        Optional<Map<String, Object>> result = client.processPayment("order-1", 99.99, "user-123");

        assertTrue(result.isEmpty());
    }
}
