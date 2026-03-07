package com.ecommerce.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Entry point for the Catalog Service microservice.
 *
 * <p>This service manages the product catalog for the eCommerce platform,
 * providing CRUD operations for products with category-based browsing
 * and full-text search capabilities.</p>
 */
@SpringBootApplication
@EnableMongoAuditing
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
