package com.ecommerce.order.repository;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    Page<Order> findByUserId(String userId, Pageable pageable);

    List<Order> findByUserIdAndStatus(String userId, OrderStatus status);

    List<Order> findByStatus(OrderStatus status);

    /**
     * Count orders containing a specific product that are in any of the given statuses.
     * Called by catalog-service to check if active orders exist before deleting a product.
     */
    long countByItemsProductIdAndStatusIn(String productId, List<OrderStatus> statuses);
}
