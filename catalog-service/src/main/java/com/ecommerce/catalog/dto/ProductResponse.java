package com.ecommerce.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outbound DTO returned to API consumers for product data.
 *
 * <p>Maps directly from the {@link com.ecommerce.catalog.model.Product} document.
 * Built via the builder pattern to keep the service layer clean.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    /** MongoDB-generated unique product identifier. */
    private String id;

    /** Human-readable product name. */
    private String name;

    /** Detailed product description. */
    private String description;

    /** Selling price. */
    private Double price;

    /** Assigned product category. */
    private String category;

    /** Current stock level. */
    private Integer stock;

    /** Identifier of the seller who owns this listing. */
    private String sellerId;

    /** Whether this product is active and visible to buyers. */
    private boolean active;

    /** Timestamp when the product was first created. */
    private LocalDateTime createdAt;

    /** Timestamp of the most recent update. */
    private LocalDateTime updatedAt;
}
