package com.ecommerce.catalog.client;

import com.ecommerce.catalog.dto.SellerInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for inter-service communication with the Auth Service.
 *
 * <p>Called by {@link com.ecommerce.catalog.service.ProductService} to fetch
 * seller information when a new product is being created. Uses graceful
 * degradation — if auth-service is unavailable, a fallback seller name is
 * used so the product creation does not fail.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.auth-url:http://auth-service}")
    private String authServiceUrl;

    /**
     * Fetches basic profile information for a seller from the Auth Service.
     *
     * @param userId the authenticated user's ID (from X-User-Id header)
     * @return a {@link SellerInfo} with the seller's username, or a fallback if unavailable
     */
    public SellerInfo getSellerInfo(String userId) {
        String url = authServiceUrl + "/api/auth/internal/users/" + userId;
        log.info("[Inter-Service] Catalog → Auth: GET {}", url);
        try {
            ResponseEntity<SellerInfo> response = restTemplate.getForEntity(url, SellerInfo.class);
            SellerInfo info = response.getBody();
            log.info("[Inter-Service] Received seller info: username='{}'",
                    info != null ? info.getUsername() : "null");
            return info != null ? info : fallback(userId);
        } catch (Exception e) {
            log.warn("[Inter-Service] Auth service unavailable for userId={}: {}", userId, e.getMessage());
            // Graceful degradation — product creation proceeds with fallback seller name
            return fallback(userId);
        }
    }

    private SellerInfo fallback(String userId) {
        return SellerInfo.builder()
                .userId(userId)
                .username("Seller-" + userId.substring(0, Math.min(6, userId.length())))
                .build();
    }
}
