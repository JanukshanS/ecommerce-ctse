package com.ecommerce.payment.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OrderServiceClientTest {

    private MockWebServer mockWebServer;
    private OrderServiceClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        client = new OrderServiceClient(baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void updateOrderPaymentStatus_shouldSucceed_whenServiceAvailable() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        assertDoesNotThrow(() -> client.updateOrderPaymentStatus("order-1", "pay-1", true));
    }

    @Test
    void updateOrderPaymentStatus_shouldNotThrow_whenServiceFails() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertDoesNotThrow(() -> client.updateOrderPaymentStatus("order-1", "pay-1", false));
    }
}
