package com.ecommerce.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Response DTO returned by the internal user-info endpoint.
 * Used by other microservices (e.g. catalog-service) to fetch
 * basic user details without needing direct DB access.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private String userId;
    private String username;
    private String email;
    private Set<String> roles;
    private boolean enabled;
}
