package com.ecommerce.cart.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CatalogServiceClientTest {

    private MockWebServer mockWebServer;
    private CatalogServiceClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        client = new CatalogServiceClient(baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getProduct_shouldReturnProduct_whenProductExists() {
        String productJson = """
                {"id":"prod-1","name":"Test Product","price":29.99}
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(productJson)
                .addHeader("Content-Type", "application/json"));

        Optional<Map<String, Object>> result = client.getProduct("prod-1");

        assertTrue(result.isPresent());
        assertEquals("prod-1", result.get().get("id"));
        assertEquals("Test Product", result.get().get("name"));
        assertEquals(29.99, ((Number) result.get().get("price")).doubleValue());
    }

    @Test
    void getProduct_shouldReturnEmpty_whenProductNotFound() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        Optional<Map<String, Object>> result = client.getProduct("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void checkStock_shouldReturnTrue_whenStockAvailable() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"available\": true}")
                .addHeader("Content-Type", "application/json"));

        boolean result = client.checkStock("prod-1", 5);

        assertTrue(result);
    }

    @Test
    void checkStock_shouldReturnFalse_whenStockNotAvailable() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"available\": false}")
                .addHeader("Content-Type", "application/json"));

        boolean result = client.checkStock("prod-1", 1000);

        assertFalse(result);
    }
}
