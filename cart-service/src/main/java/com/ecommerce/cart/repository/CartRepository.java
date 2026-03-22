package com.ecommerce.cart.repository;

import com.ecommerce.cart.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<Cart, String> {

    Optional<Cart> findByUserId(String userId);

    /** Find all carts that contain at least one item for the given product. */
    List<Cart> findByItemsProductId(String productId);

    /** Count how many users currently have the given product in their cart. */
    long countByItemsProductId(String productId);
}
