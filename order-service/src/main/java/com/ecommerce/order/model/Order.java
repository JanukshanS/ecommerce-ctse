package com.ecommerce.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String userId;

    private List<OrderItem> items;

    private Double totalAmount;

    private OrderStatus status;

    private PaymentStatus paymentStatus;

    private String paymentId;

    private String shippingAddress;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
