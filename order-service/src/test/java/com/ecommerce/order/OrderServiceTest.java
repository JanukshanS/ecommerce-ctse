package com.ecommerce.order;

import com.ecommerce.order.client.CartServiceClient;
import com.ecommerce.order.client.PaymentServiceClient;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.model.PaymentStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private CartServiceClient cartServiceClient;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private final String userId = "user123";
    private final String orderId = "order001";

    @BeforeEach
    void setUp() {
        OrderItem orderItem = OrderItem.builder()
                .productId("prod001")
                .productName("Test Product")
                .price(29.99)
                .quantity(2)
                .subtotal(59.98)
                .build();

        testOrder = Order.builder()
                .id(orderId)
                .userId(userId)
                .items(Arrays.asList(orderItem))
                .totalAmount(59.98)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .shippingAddress("123 Test St, Test City")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createOrder - should create order successfully with correct total amount")
    void createOrder_ShouldCreateOrderSuccessfully() {
        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productId("prod001")
                .productName("Test Product")
                .price(29.99)
                .quantity(2)
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(Arrays.asList(itemRequest))
                .shippingAddress("123 Test St, Test City")
                .notes("Please deliver ASAP")
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(orderId);
            return order;
        });
        when(paymentServiceClient.processPayment(any(), any(), any())).thenReturn(
                Optional.of(Map.of("id", "pay001", "status", "SUCCESS")));

        OrderResponse response = orderService.createOrder(userId, request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertThat(response.getTotalAmount()).isEqualTo(59.98);
        assertEquals("123 Test St, Test City", response.getShippingAddress());
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder - should calculate total amount correctly for multiple items")
    void createOrder_ShouldCalculateTotalAmountCorrectly() {
        OrderItemRequest item1 = OrderItemRequest.builder()
                .productId("prod001")
                .productName("Product 1")
                .price(10.00)
                .quantity(2)
                .build();
        OrderItemRequest item2 = OrderItemRequest.builder()
                .productId("prod002")
                .productName("Product 2")
                .price(25.50)
                .quantity(3)
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(Arrays.asList(item1, item2))
                .shippingAddress("456 Main St")
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentServiceClient.processPayment(any(), any(), any())).thenReturn(Optional.empty());

        OrderResponse response = orderService.createOrder(userId, request);

        assertNotNull(response);
        assertThat(response.getTotalAmount()).isEqualTo(96.50);
        assertThat(response.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("getUserOrders - should return paged orders for user")
    void getUserOrders_ShouldReturnPagedOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));

        when(orderRepository.findByUserId(userId, pageable)).thenReturn(orderPage);

        Page<OrderResponse> result = orderService.getUserOrders(userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(orderId, result.getContent().get(0).getId());
        assertEquals(userId, result.getContent().get(0).getUserId());
        verify(orderRepository, times(1)).findByUserId(userId, pageable);
    }

    @Test
    @DisplayName("getUserOrders - should return empty page when user has no orders")
    void getUserOrders_ShouldReturnEmptyPageWhenNoOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());

        when(orderRepository.findByUserId(userId, pageable)).thenReturn(emptyPage);

        Page<OrderResponse> result = orderService.getUserOrders(userId, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("cancelOrder - should cancel pending order successfully")
    void cancelOrder_ShouldCancelPendingOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder(orderId, userId);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("cancelOrder - should throw exception when order not found")
    void cancelOrder_WhenOrderNotFound_ShouldThrowException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> orderService.cancelOrder(orderId, userId));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("cancelOrder - should throw exception when order belongs to different user")
    void cancelOrder_WhenOrderBelongsToDifferentUser_ShouldThrowException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThrows(RuntimeException.class, () -> orderService.cancelOrder(orderId, "differentUser"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("cancelOrder - should throw exception when order is already shipped")
    void cancelOrder_WhenOrderIsShipped_ShouldThrowException() {
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThrows(RuntimeException.class, () -> orderService.cancelOrder(orderId, userId));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("cancelOrder - should throw exception when order is already delivered")
    void cancelOrder_WhenOrderIsDelivered_ShouldThrowException() {
        testOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThrows(RuntimeException.class, () -> orderService.cancelOrder(orderId, userId));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("getOrderById - should return order when found and belongs to user")
    void getOrderById_ShouldReturnOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        OrderResponse response = orderService.getOrderById(orderId, userId);

        assertNotNull(response);
        assertEquals(orderId, response.getId());
        assertEquals(userId, response.getUserId());
    }

    @Test
    @DisplayName("getOrderById - should throw exception when order not found")
    void getOrderById_WhenNotFound_ShouldThrowException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> orderService.getOrderById(orderId, userId));
    }
}
