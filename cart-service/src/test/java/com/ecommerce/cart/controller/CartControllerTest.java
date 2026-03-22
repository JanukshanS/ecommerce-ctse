package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CartController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    private static CartResponse sampleCart(String userId) {
        return CartResponse.builder()
                .cartId("cart-1")
                .userId(userId)
                .items(Collections.emptyList())
                .totalAmount(0.0)
                .itemCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/cart — X-User-Id header only returns 200")
    void getCart_headerOnly_returnsOk() throws Exception {
        when(cartService.getCart("user-a")).thenReturn(sampleCart("user-a"));

        mockMvc.perform(get("/api/cart")
                        .header("X-User-Id", "user-a")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-a"));

        verify(cartService).getCart(eq("user-a"));
    }

    @Test
    @DisplayName("GET /api/cart — userId query only returns 200")
    void getCart_queryOnly_returnsOk() throws Exception {
        when(cartService.getCart("user-b")).thenReturn(sampleCart("user-b"));

        mockMvc.perform(get("/api/cart")
                        .param("userId", "user-b")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-b"));

        verify(cartService).getCart(eq("user-b"));
    }

    @Test
    @DisplayName("GET /api/cart — matching header and query returns 200")
    void getCart_matchingHeaderAndQuery_returnsOk() throws Exception {
        when(cartService.getCart("user-c")).thenReturn(sampleCart("user-c"));

        mockMvc.perform(get("/api/cart")
                        .header("X-User-Id", "user-c")
                        .param("userId", "user-c")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(cartService).getCart(eq("user-c"));
    }

    @Test
    @DisplayName("GET /api/cart — trims whitespace when comparing header and query")
    void getCart_matchingHeaderAndQueryWithWhitespace_returnsOk() throws Exception {
        when(cartService.getCart("user-d")).thenReturn(sampleCart("user-d"));

        mockMvc.perform(get("/api/cart")
                        .header("X-User-Id", "  user-d  ")
                        .param("userId", "user-d")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(cartService).getCart(eq("user-d"));
    }

    @Test
    @DisplayName("GET /api/cart — no header and no query returns 400")
    void getCart_missingUserId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/cart").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).getCart(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("GET /api/cart — mismatched header and query returns 400")
    void getCart_mismatchedHeaderAndQuery_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("X-User-Id", "user-1")
                        .param("userId", "user-2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).getCart(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("DELETE /api/cart/items/product/{productId} — internal cleanup returns 204")
    void removeProductFromAllCarts_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/cart/items/product/prod-99"))
                .andExpect(status().isNoContent());

        verify(cartService).removeProductFromAllCarts(eq("prod-99"));
    }

    @Test
    @DisplayName("GET /api/cart/product/{productId}/count — returns cartCount")
    void getProductDemandCount_returnsJson() throws Exception {
        when(cartService.countCartsWithProduct("p1")).thenReturn(7L);

        mockMvc.perform(get("/api/cart/product/p1/count")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("p1"))
                .andExpect(jsonPath("$.cartCount").value(7));

        verify(cartService).countCartsWithProduct(eq("p1"));
    }
}
