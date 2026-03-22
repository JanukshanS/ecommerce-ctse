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
}
