package com.ecommerce.catalog.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for inter-service communication with the Cart Service.
 *
 * <p>Called by {@link com.ecommerce.catalog.service.ProductService} when a
 * product is soft-deleted to notify the Cart Service to remove all cart items
 * that reference this product. This prevents stale cart entries pointing to
 * deactivated products.</p>
 *
 * <p>Uses graceful degradation — if the Cart Service is unavailable, the
 * product deletion still completes successfully and the cart cleanup is
 * skipped (cart items will be filtered out at display time instead).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.cart-url:http://cart-service}")
    private String cartServiceUrl;

    /**
     * Notifies the Cart Service to remove all cart items for the given product.
     *
     * @param productId the ID of the deleted/deactivated product
     */
    public void removeProductFromCarts(String productId) {
        String url = cartServiceUrl + "/api/cart/items/product/" + productId;
        log.info("[Inter-Service] Catalog → Cart: DELETE {}", url);
        try {
            restTemplate.delete(url);
            log.info("[Inter-Service] Cart service notified: product {} removed from all carts", productId);
        } catch (Exception e) {
            // Graceful degradation — product deletion proceeds regardless
            log.warn("[Inter-Service] Cart service unavailable, skipping cart cleanup for product {}: {}",
                    productId, e.getMessage());
        }
    }

    /**
     * Calls cart-service to find how many users currently have this product in their cart.
     * Used during stock-check to return a real-time demand metric.
     *
     * @param productId the product to check demand for
     * @return number of users who have this product in their cart, 0 if unavailable
     */
    public long getDemandCount(String productId) {
        String url = cartServiceUrl + "/api/cart/product/" + productId + "/count";
        log.info("[Inter-Service] Catalog → Cart: GET {}", url);
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> response =
                    restTemplate.getForObject(url, java.util.Map.class);
            if (response != null && response.containsKey("cartCount")) {
                long count = ((Number) response.get("cartCount")).longValue();
                log.info("[Inter-Service] Product {} is currently in {} user carts", productId, count);
                return count;
            }
        } catch (Exception e) {
            log.warn("[Inter-Service] Cart service unavailable for demand count of product {}: {}",
                    productId, e.getMessage());
        }
        return 0;
    }
}
