package com.ecommerce.order.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CartServiceClientTest {

    private MockWebServer mockWebServer;
    private CartServiceClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        client = new CartServiceClient(baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void clearCart_shouldSucceed_whenServiceAvailable() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        assertDoesNotThrow(() -> client.clearCart("user-123"));
    }

    @Test
    void clearCart_shouldNotThrow_whenServiceFails() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertDoesNotThrow(() -> client.clearCart("user-123"));
    }
}
