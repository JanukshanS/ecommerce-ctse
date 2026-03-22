package com.ecommerce.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO received from the Auth Service when catalog looks up a seller's profile.
 * Maps to the response body of GET /api/auth/internal/users/{userId}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerInfo {
    private String userId;
    private String username;
    private String email;
}
