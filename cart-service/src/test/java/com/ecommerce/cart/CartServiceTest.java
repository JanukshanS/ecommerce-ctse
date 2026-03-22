package com.ecommerce.cart;

import com.ecommerce.cart.client.CatalogServiceClient;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CatalogServiceClient catalogServiceClient;

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

        when(catalogServiceClient.getProduct("prod002")).thenReturn(
                Optional.of(Map.of("id", "prod002", "name", "New Product", "price", 19.99, "stock", 10)));
        when(catalogServiceClient.checkStock("prod002", 1)).thenReturn(true);
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

        when(catalogServiceClient.getProduct("prod001")).thenReturn(
                Optional.of(Map.of("id", "prod001", "name", "Test Product", "price", 29.99, "stock", 10)));
        when(catalogServiceClient.checkStock("prod001", 1)).thenReturn(true);
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

        when(catalogServiceClient.getProduct("prod001")).thenReturn(
                Optional.of(Map.of("id", "prod001", "name", "Test Product", "price", 29.99, "stock", 50)));
        when(catalogServiceClient.checkStock("prod001", 5)).thenReturn(true);
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

        verify(cartRepository, times(1)).save(argThat(cart ->
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

    @Test
    @DisplayName("getCart - should return existing cart")
    void getCart_WhenCartExists_ShouldReturnMappedResponse() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));

        CartResponse response = cartService.getCart(userId);

        assertNotNull(response);
        assertEquals("cart001", response.getCartId());
        assertEquals(userId, response.getUserId());
        assertEquals(1, response.getItemCount());
        assertThat(response.getTotalAmount()).isEqualTo(59.98);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("getCart - should create and persist empty cart when none exists")
    void getCart_WhenCartMissing_ShouldCreateEmptyCart() {
        Cart newCart = Cart.builder()
                .id("new-id")
                .userId(userId)
                .items(new ArrayList<>())
                .totalAmount(0.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        CartResponse response = cartService.getCart(userId);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(0, response.getItemCount());
        assertThat(response.getTotalAmount()).isEqualTo(0.0);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("addItem - should throw when product not found in catalog")
    void addItem_WhenProductNotFound_ShouldThrow() {
        AddToCartRequest request = AddToCartRequest.builder()
                .productId("missing")
                .productName("X")
                .price(1.0)
                .quantity(1)
                .build();

        when(catalogServiceClient.getProduct("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cartService.addItem(userId, request));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("addItem - should throw when insufficient stock")
    void addItem_WhenInsufficientStock_ShouldThrow() {
        AddToCartRequest request = AddToCartRequest.builder()
                .productId("prod002")
                .productName("Y")
                .price(10.0)
                .quantity(100)
                .build();

        when(catalogServiceClient.getProduct("prod002")).thenReturn(
                Optional.of(Map.of("id", "prod002", "name", "Y", "price", 10.0, "stock", 5)));
        when(catalogServiceClient.checkStock("prod002", 100)).thenReturn(false);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));

        assertThrows(RuntimeException.class, () -> cartService.addItem(userId, request));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("removeProductFromAllCarts - should strip product from every cart and save")
    void removeProductFromAllCarts_ShouldUpdateEachCart() {
        CartItem other = CartItem.builder()
                .productId("keep")
                .productName("Keep")
                .price(5.0)
                .quantity(1)
                .build();

        Cart cart1 = Cart.builder()
                .id("c1")
                .userId("u1")
                .items(new ArrayList<>(Arrays.asList(testCartItem, other)))
                .totalAmount(64.98)
                .build();

        List<Cart> carts = new ArrayList<>(Collections.singletonList(cart1));
        when(cartRepository.findByItemsProductId("prod001")).thenReturn(carts);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.removeProductFromAllCarts("prod001");

        assertThat(cart1.getItems()).hasSize(1);
        assertEquals("keep", cart1.getItems().get(0).getProductId());
        verify(cartRepository, times(1)).save(eq(cart1));
    }

    @Test
    @DisplayName("removeProductFromAllCarts - no carts with product does nothing")
    void removeProductFromAllCarts_WhenNoCarts_ShouldNotSave() {
        when(cartRepository.findByItemsProductId("ghost")).thenReturn(Collections.emptyList());

        cartService.removeProductFromAllCarts("ghost");

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("countCartsWithProduct - delegates to repository")
    void countCartsWithProduct_ShouldReturnRepositoryCount() {
        when(cartRepository.countByItemsProductId("hot")).thenReturn(42L);

        long count = cartService.countCartsWithProduct("hot");

        assertEquals(42L, count);
        verify(cartRepository).countByItemsProductId("hot");
    }
}
