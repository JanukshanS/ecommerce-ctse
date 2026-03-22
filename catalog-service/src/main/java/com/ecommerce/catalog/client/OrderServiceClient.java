package com.ecommerce.catalog.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for inter-service communication with the Order Service (Asath).
 *
 * <p>Called by {@link com.ecommerce.catalog.service.ProductService} before
 * deleting a product to check whether any active orders exist for it.
 * If active orders are found, the deletion is still allowed but a warning
 * is included in the response to alert the seller.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.order-url:http://order-service}")
    private String orderServiceUrl;

    /**
     * Checks how many active (non-cancelled, non-delivered) orders contain
     * the given product. Returns 0 if order-service is unavailable.
     *
     * @param productId the ID of the product to check
     * @return count of active orders containing this product
     */
    public long getActiveOrderCount(String productId) {
        String url = orderServiceUrl + "/api/orders/product/" + productId + "/active-count";
        log.info("[Inter-Service] Catalog → Order: GET {}", url);
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> response =
                    restTemplate.getForObject(url, java.util.Map.class);
            if (response != null && response.containsKey("activeOrderCount")) {
                long count = ((Number) response.get("activeOrderCount")).longValue();
                log.info("[Inter-Service] Product {} has {} active orders", productId, count);
                return count;
            }
        } catch (Exception e) {
            log.warn("[Inter-Service] Order service unavailable for product {}: {}", productId, e.getMessage());
        }
        return 0;
    }
}
