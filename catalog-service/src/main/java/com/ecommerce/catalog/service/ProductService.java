package com.ecommerce.catalog.service;

import com.ecommerce.catalog.dto.ProductRequest;
import com.ecommerce.catalog.dto.ProductResponse;
import com.ecommerce.catalog.model.Product;
import com.ecommerce.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Business logic layer for product catalog operations.
 *
 * <p>This service enforces ownership rules (a seller may only modify/delete
 * their own products), maps between the domain model and response DTOs, and
 * delegates all persistence to {@link ProductRepository}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    // -----------------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------------

    /**
     * Persists a new product listing.
     *
     * @param request  validated inbound DTO containing product details
     * @param sellerId identifier of the authenticated seller creating the listing
     * @return the newly created product represented as a {@link ProductResponse}
     * @throws IllegalArgumentException if a product with the same name already exists
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request, String sellerId) {
        log.info("Creating product '{}' for seller '{}'", request.getName(), sellerId);

        if (productRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "A product with the name '" + request.getName() + "' already exists.");
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .stock(request.getStock())
                .sellerId(sellerId)
                .active(true)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created with id '{}'", saved.getId());
        return toResponse(saved);
    }

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    /**
     * Returns a paginated view of all active products.
     *
     * @param pageable pagination and sorting parameters
     * @return page of {@link ProductResponse} instances
     */
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.debug("Fetching all active products, page={}", pageable.getPageNumber());
        return productRepository.findByActiveTrue(pageable).map(this::toResponse);
    }

    /**
     * Looks up a single product by its unique identifier.
     *
     * @param id the MongoDB document id
     * @return the matching product as a {@link ProductResponse}
     * @throws RuntimeException if no product with the supplied id exists
     */
    public ProductResponse getProductById(String id) {
        log.debug("Fetching product by id '{}'", id);
        Product product = findByIdOrThrow(id);
        return toResponse(product);
    }

    /**
     * Returns a paginated list of active products in the specified category.
     *
     * @param category the category name to filter on
     * @param pageable pagination and sorting parameters
     * @return page of {@link ProductResponse} instances
     */
    public Page<ProductResponse> getProductsByCategory(String category, Pageable pageable) {
        log.debug("Fetching products for category '{}'", category);
        return productRepository.findByCategoryAndActive(category, true, pageable)
                .map(this::toResponse);
    }

    /**
     * Performs a case-insensitive substring search against product names.
     *
     * @param query    the search string
     * @param pageable pagination and sorting parameters
     * @return page of {@link ProductResponse} instances whose names match the query
     */
    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
        log.debug("Searching products with query '{}'", query);
        return productRepository.findByNameContainingIgnoreCase(query, pageable)
                .map(this::toResponse);
    }

    // -----------------------------------------------------------------------
    // Update
    // -----------------------------------------------------------------------

    /**
     * Updates an existing product.
     *
     * <p>Only the seller who created the product may update it.</p>
     *
     * @param id       the product identifier
     * @param request  validated DTO with updated field values
     * @param sellerId the seller attempting the update
     * @return the updated product as a {@link ProductResponse}
     * @throws RuntimeException if the product is not found
     * @throws RuntimeException if the requesting seller does not own the product
     */
    @Transactional
    public ProductResponse updateProduct(String id, ProductRequest request, String sellerId) {
        log.info("Updating product '{}' by seller '{}'", id, sellerId);

        Product product = findByIdOrThrow(id);
        assertOwnership(product, sellerId, "update");

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStock(request.getStock());
        product.setUpdatedAt(LocalDateTime.now());

        Product updated = productRepository.save(product);
        log.info("Product '{}' updated successfully", id);
        return toResponse(updated);
    }

    // -----------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------

    /**
     * Soft-deletes a product by setting its {@code active} flag to {@code false}.
     *
     * <p>Only the seller who created the product may delete it.</p>
     *
     * @param id       the product identifier
     * @param sellerId the seller attempting the deletion
     * @throws RuntimeException if the product is not found
     * @throws RuntimeException if the requesting seller does not own the product
     */
    @Transactional
    public void deleteProduct(String id, String sellerId) {
        log.info("Deleting product '{}' by seller '{}'", id, sellerId);

        Product product = findByIdOrThrow(id);
        assertOwnership(product, sellerId, "delete");

        product.setActive(false);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Product '{}' soft-deleted", id);
    }

    // -----------------------------------------------------------------------
    // Stock check
    // -----------------------------------------------------------------------

    /**
     * Verifies whether sufficient stock is available for a given product.
     *
     * @param id       the product identifier
     * @param quantity the desired purchase quantity
     * @return {@code true} if the current stock is greater than or equal to
     *         {@code quantity}; {@code false} otherwise
     * @throws RuntimeException if the product is not found or is inactive
     */
    public boolean checkStock(String id, int quantity) {
        log.debug("Checking stock for product '{}', requested quantity={}", id, quantity);

        Product product = findByIdOrThrow(id);

        if (!product.isActive()) {
            throw new RuntimeException("Product with id '" + id + "' is not available.");
        }

        boolean sufficient = product.getStock() >= quantity;
        log.debug("Stock check for product '{}': stock={}, requested={}, sufficient={}",
                id, product.getStock(), quantity, sufficient);
        return sufficient;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Retrieves a product by id or throws a descriptive {@link RuntimeException}.
     */
    private Product findByIdOrThrow(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    /**
     * Ensures the given seller owns the product; throws if not.
     *
     * @param product    the product to check ownership of
     * @param sellerId   the seller attempting the operation
     * @param operation  a human-readable name for the operation (used in the error message)
     */
    private void assertOwnership(Product product, String sellerId, String operation) {
        if (!product.getSellerId().equals(sellerId)) {
            throw new RuntimeException(
                    "Seller '" + sellerId + "' is not authorised to " + operation
                            + " product '" + product.getId() + "'.");
        }
    }

    /**
     * Maps a {@link Product} domain object to a {@link ProductResponse} DTO.
     *
     * @param product the product to map
     * @return the corresponding response DTO
     */
    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory())
                .stock(product.getStock())
                .sellerId(product.getSellerId())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
