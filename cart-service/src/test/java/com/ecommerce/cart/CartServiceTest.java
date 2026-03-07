package com.ecommerce.cart;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.model.Cart;
import com.ecommerce.cart.model.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.cart.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    private Cart testCart;
    private CartItem testCartItem;
    private final String userId = "user123";

    @BeforeEach
    void setUp() {
        testCartItem = CartItem.builder()
                .productId("prod001")
                .productName("Test Product")
                .price(29.99)
                .quantity(2)
                .build();

        testCart = Cart.builder()
                .id("cart001")
                .userId(userId)
                .items(new ArrayList<>(Arrays.asList(testCartItem)))
                .totalAmount(59.98)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("addItem - should add new item when cart exists")
    void addItem_WhenCartExists_ShouldAddNewItem() {
        AddToCartRequest request = AddToCartRequest.builder()
                .productId("prod002")
                .productName("New Product")
                .price(19.99)
                .quantity(1)
                .build();

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addItem(userId, request);

        assertNotNull(response);
        assertEquals(2, response.getItems().size());
        assertThat(response.getTotalAmount()).isEqualTo(79.97);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("addItem - should create new cart when cart does not exist")
    void addItem_WhenCartDoesNotExist_ShouldCreateNewCartAndAddItem() {
        AddToCartRequest request = AddToCartRequest.builder()
                .productId("prod001")
                .productName("Test Product")
                .price(29.99)
                .quantity(1)
                .build();

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            if (cart.getId() == null) {
                cart.setId("newCart001");
            }
            return cart;
        });

        CartResponse response = cartService.addItem(userId, request);

        assertNotNull(response);
        assertThat(response.getItems()).hasSize(1);
        assertEquals("prod001", response.getItems().get(0).getProductId());
        verify(cartRepository, times(2)).save(any(Cart.class));
    }

    @Test
    @DisplayName("addItem - should increment quantity when item already exists in cart")
    void addItem_WhenItemAlreadyInCart_ShouldIncrementQuantity() {
        AddToCartRequest request = AddToCartRequest.builder()
                .productId("prod001")
                .productName("Test Product")
                .price(29.99)
                .quantity(3)
                .build();

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addItem(userId, request);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(5, response.getItems().get(0).getQuantity());
        assertThat(response.getTotalAmount()).isEqualTo(149.95);
    }

    @Test
    @DisplayName("removeItem - should remove item from cart when item exists")
    void removeItem_WhenItemExists_ShouldRemoveItemFromCart() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.removeItem(userId, "prod001");

        assertNotNull(response);
        assertEquals(0, response.getItems().size());
        assertEquals(0.0, response.getTotalAmount());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("removeItem - should throw exception when cart not found")
    void removeItem_WhenCartNotFound_ShouldThrowException() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cartService.removeItem(userId, "prod001"));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("clearCart - should empty cart when cart exists")
    void clearCart_WhenCartExists_ShouldEmptyCart() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.clearCart(userId);

        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(cartRepository).save(argThat(cart ->
                cart.getItems().isEmpty() && cart.getTotalAmount() == 0.0));
    }

    @Test
    @DisplayName("clearCart - should throw exception when cart not found")
    void clearCart_WhenCartNotFound_ShouldThrowException() {
        when(cartRepository.findByUserId(anyString())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cartService.clearCart(userId));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("updateItem - should update quantity when item exists")
    void updateItem_WhenItemExists_ShouldUpdateQuantity() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.updateItem(userId, "prod001", 5);

        assertNotNull(response);
        assertEquals(5, response.getItems().get(0).getQuantity());
        assertThat(response.getTotalAmount()).isEqualTo(149.95);
    }

    @Test
    @DisplayName("updateItem - should remove item when quantity set to 0")
    void updateItem_WhenQuantityIsZero_ShouldRemoveItem() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.updateItem(userId, "prod001", 0);

        assertNotNull(response);
        assertEquals(0, response.getItems().size());
        assertEquals(0.0, response.getTotalAmount());
    }

    private <T> T argThat(org.mockito.ArgumentMatcher<T> matcher) {
        return org.mockito.Mockito.argThat(matcher);
    }
}
