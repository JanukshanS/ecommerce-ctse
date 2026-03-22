package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.UpdateCartItemRequest;
import com.ecommerce.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Returns the cart for a user. Identify the user with:
     * <ul>
     *   <li>{@code X-User-Id} header (set by the API Gateway from JWT), and/or</li>
     *   <li>{@code userId} query parameter (e.g. {@code GET /api/cart?userId=...})</li>
     * </ul>
     * If both are present they must match. At least one is required.
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String userIdQuery) {
        String userId = resolveUserIdForGet(headerUserId, userIdQuery);
        log.info("Getting cart for user: {}", userId);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    /**
     * Resolves user id from header (gateway) and/or query param. Prevents mismatch when both are sent
     * (e.g. JWT user A with {@code ?userId=B} is rejected).
     */
    private static String resolveUserIdForGet(String headerUserId, String userIdQuery) {
        boolean hasHeader = StringUtils.hasText(headerUserId);
        boolean hasQuery = StringUtils.hasText(userIdQuery);
        if (!hasHeader && !hasQuery) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide userId via X-User-Id header and/or userId query parameter");
        }
        if (hasHeader && hasQuery && !headerUserId.trim().equals(userIdQuery.trim())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "X-User-Id header and userId query parameter must refer to the same user");
        }
        return hasHeader ? headerUserId.trim() : userIdQuery.trim();
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddToCartRequest request) {
        log.info("Adding item to cart for user: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItem(userId, request));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        log.info("Updating cart item for user: {}, productId: {}", userId, productId);
        return ResponseEntity.ok(cartService.updateItem(userId, productId, request.getQuantity()));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String productId) {
        log.info("Removing item from cart for user: {}, productId: {}", userId, productId);
        return ResponseEntity.ok(cartService.removeItem(userId, productId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @RequestHeader("X-User-Id") String userId) {
        log.info("Clearing cart for user: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Internal endpoint — called by catalog-service when a product is deleted.
     * Removes the product from ALL users' carts across the platform.
     * No X-User-Id header required (operates across all users).
     */
    @DeleteMapping("/items/product/{productId}")
    public ResponseEntity<Void> removeProductFromAllCarts(@PathVariable String productId) {
        log.info("[Internal] Removing product {} from all carts", productId);
        cartService.removeProductFromAllCarts(productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Internal endpoint — called by catalog-service during stock-check.
     * Returns the number of users who currently have this product in their cart.
     * This demand metric helps catalog understand product popularity.
     */
    @GetMapping("/product/{productId}/count")
    public ResponseEntity<java.util.Map<String, Object>> getProductDemandCount(
            @PathVariable String productId) {
        long count = cartService.countCartsWithProduct(productId);
        log.info("[Internal] Product {} is in {} carts", productId, count);
        return ResponseEntity.ok(java.util.Map.of(
                "productId", productId,
                "cartCount", count
        ));
    }
}
