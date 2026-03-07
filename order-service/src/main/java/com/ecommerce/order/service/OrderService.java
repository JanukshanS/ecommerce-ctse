package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.UpdateOrderStatusRequest;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.model.PaymentStatus;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        List<OrderItem> orderItems = request.getItems().stream()
                .map(itemRequest -> OrderItem.builder()
                        .productId(itemRequest.getProductId())
                        .productName(itemRequest.getProductName())
                        .price(itemRequest.getPrice())
                        .quantity(itemRequest.getQuantity())
                        .subtotal(Math.round(itemRequest.getPrice() * itemRequest.getQuantity() * 100.0) / 100.0)
                        .build())
                .collect(Collectors.toList());

        double totalAmount = orderItems.stream()
                .mapToDouble(OrderItem::getSubtotal)
                .sum();
        totalAmount = Math.round(totalAmount * 100.0) / 100.0;

        Order order = Order.builder()
                .userId(userId)
                .items(orderItems)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {} for user: {}", savedOrder.getId(), userId);
        return mapToResponse(savedOrder);
    }

    public Page<OrderResponse> getUserOrders(String userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    public OrderResponse getOrderById(String id, String userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied: Order does not belong to user");
        }

        return mapToResponse(order);
    }

    public OrderResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }
        if (request.getPaymentStatus() != null) {
            order.setPaymentStatus(request.getPaymentStatus());
        }
        if (request.getPaymentId() != null) {
            order.setPaymentId(request.getPaymentId());
        }
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        log.info("Order status updated for orderId: {}", orderId);
        return mapToResponse(savedOrder);
    }

    public OrderResponse cancelOrder(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied: Order does not belong to user");
        }

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        log.info("Order cancelled: {} for user: {}", orderId, userId);
        return mapToResponse(savedOrder);
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .items(order.getItems())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentId(order.getPaymentId())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
