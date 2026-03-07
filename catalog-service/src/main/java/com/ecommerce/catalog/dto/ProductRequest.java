package com.ecommerce.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Inbound DTO used when creating or updating a product.
 *
 * <p>All fields carry Bean Validation annotations so that the controller
 * can reject malformed requests before they reach the service layer.</p>
 */
@Data
public class ProductRequest {

    /**
     * Product name — must not be blank, max 200 characters.
     */
    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    /**
     * Product description — must not be blank, max 2000 characters.
     */
    @NotBlank(message = "Product description is required")
    @Size(max = 2000, message = "Product description must not exceed 2000 characters")
    private String description;

    /**
     * Selling price — must not be null and must be a strictly positive number.
     */
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be a positive value")
    private Double price;

    /**
     * Product category — must not be blank.
     */
    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    /**
     * Available stock count — must not be null and must be zero or greater.
     */
    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be zero or greater")
    private Integer stock;
}
