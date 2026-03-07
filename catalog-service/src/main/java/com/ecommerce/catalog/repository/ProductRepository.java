package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for {@link Product} documents.
 *
 * <p>Provides standard CRUD operations inherited from {@link MongoRepository}
 * plus a set of derived query methods for common access patterns used by the
 * service layer.</p>
 */
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    /**
     * Returns a paginated list of products belonging to the given category.
     *
     * @param category the category name to filter by (case-sensitive)
     * @param pageable pagination and sorting parameters
     * @return page of matching products
     */
    Page<Product> findByCategory(String category, Pageable pageable);

    /**
     * Performs a case-insensitive substring search on the product name.
     *
     * @param name     the substring to search for within product names
     * @param pageable pagination and sorting parameters
     * @return page of products whose names contain the supplied string
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Returns all products owned by a given seller.
     *
     * @param sellerId the seller's identifier
     * @return list of products belonging to the seller
     */
    List<Product> findBySellerId(String sellerId);

    /**
     * Returns a paginated list of products that are currently active.
     *
     * @param pageable pagination and sorting parameters
     * @return page of active products
     */
    Page<Product> findByActiveTrue(Pageable pageable);

    /**
     * Returns a paginated list of products matching both category and active flag.
     *
     * @param category the category name to filter by
     * @param active   whether the product must be active
     * @param pageable pagination and sorting parameters
     * @return page of matching products
     */
    Page<Product> findByCategoryAndActive(String category, boolean active, Pageable pageable);

    /**
     * Checks whether a product with the given name already exists.
     *
     * @param name the product name to check
     * @return {@code true} if a product with that name exists; {@code false} otherwise
     */
    boolean existsByName(String name);
}
