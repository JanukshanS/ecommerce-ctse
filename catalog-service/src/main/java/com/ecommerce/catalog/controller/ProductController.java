package com.ecommerce.catalog.controller;

import com.ecommerce.catalog.dto.ProductRequest;
import com.ecommerce.catalog.dto.ProductResponse;
import com.ecommerce.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller exposing the product catalog API.
 *
 * <p>All endpoints are rooted at {@code /api/catalog/products}.
 * Write operations (POST, PUT, DELETE) expect the API Gateway to forward the
 * authenticated user's identifier in the {@code X-User-Id} request header.</p>
 */
@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Product Catalog", description = "Endpoints for browsing and managing the product catalog")
public class ProductController {

    private static final String SELLER_ID_HEADER = "X-User-Id";

    private final ProductService productService;

    // -----------------------------------------------------------------------
    // POST /api/catalog/products
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Create a new product",
            description = "Creates a new product listing. Requires an authenticated seller; "
                    + "the seller's id must be supplied via the X-User-Id header."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "409", description = "Product with the same name already exists")
    })
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request,
            @Parameter(description = "Authenticated seller id forwarded by the API Gateway")
            @RequestHeader(SELLER_ID_HEADER) String sellerId) {

        log.info("POST /api/catalog/products — seller='{}'", sellerId);
        ProductResponse created = productService.createProduct(request, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------------------------------------
    // GET /api/catalog/products
    // -----------------------------------------------------------------------

    @Operation(
            summary = "List all active products",
            description = "Returns a paginated list of all active products. "
                    + "Use the 'page', 'size', and 'sort' query parameters for pagination/sorting."
    )
    @ApiResponse(responseCode = "200", description = "Paginated product list returned")
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("GET /api/catalog/products page={}", pageable.getPageNumber());
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    // -----------------------------------------------------------------------
    // GET /api/catalog/products/{id}
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Get a product by id",
            description = "Returns the full details of a single product identified by its unique id."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Product unique identifier") @PathVariable String id) {

        log.debug("GET /api/catalog/products/{}", id);
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // -----------------------------------------------------------------------
    // GET /api/catalog/products/category/{category}
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Browse products by category",
            description = "Returns a paginated list of active products belonging to a specific category."
    )
    @ApiResponse(responseCode = "200", description = "Products returned for the requested category")
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @Parameter(description = "Category name") @PathVariable String category,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {

        log.debug("GET /api/catalog/products/category/{}", category);
        return ResponseEntity.ok(productService.getProductsByCategory(category, pageable));
    }

    // -----------------------------------------------------------------------
    // GET /api/catalog/products/search?q=
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Search products by name",
            description = "Performs a case-insensitive substring search against product names."
    )
    @ApiResponse(responseCode = "200", description = "Search results returned")
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @Parameter(description = "Search query string", required = true)
            @RequestParam("q") String query,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {

        log.debug("GET /api/catalog/products/search?q={}", query);
        return ResponseEntity.ok(productService.searchProducts(query, pageable));
    }

    // -----------------------------------------------------------------------
    // PUT /api/catalog/products/{id}
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Update a product",
            description = "Updates an existing product. Only the seller who created the product "
                    + "may perform this operation (enforced via the X-User-Id header)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Seller is not the owner of this product"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product unique identifier") @PathVariable String id,
            @Valid @RequestBody ProductRequest request,
            @Parameter(description = "Authenticated seller id forwarded by the API Gateway")
            @RequestHeader(SELLER_ID_HEADER) String sellerId) {

        log.info("PUT /api/catalog/products/{} — seller='{}'", id, sellerId);
        return ResponseEntity.ok(productService.updateProduct(id, request, sellerId));
    }

    // -----------------------------------------------------------------------
    // DELETE /api/catalog/products/{id}
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Delete (soft) a product",
            description = "Deactivates a product listing. Only the seller who created the product "
                    + "may perform this operation (enforced via the X-User-Id header)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Seller is not the owner of this product"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product unique identifier") @PathVariable String id,
            @Parameter(description = "Authenticated seller id forwarded by the API Gateway")
            @RequestHeader(SELLER_ID_HEADER) String sellerId) {

        log.info("DELETE /api/catalog/products/{} — seller='{}'", id, sellerId);
        productService.deleteProduct(id, sellerId);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // GET /api/catalog/products/{id}/stock-check?quantity=
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Check available stock",
            description = "Returns whether the requested quantity is available for the given product."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock check result returned"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}/stock-check")
    public ResponseEntity<Map<String, Object>> checkStock(
            @Parameter(description = "Product unique identifier") @PathVariable String id,
            @Parameter(description = "Quantity to check against available stock", required = true)
            @RequestParam @Min(value = 1, message = "Quantity must be at least 1") int quantity) {

        log.debug("GET /api/catalog/products/{}/stock-check?quantity={}", id, quantity);
        boolean available = productService.checkStock(id, quantity);
        return ResponseEntity.ok(Map.of(
                "productId", id,
                "requestedQuantity", quantity,
                "available", available
        ));
    }
}
