package com.ecommerce.cart.service;

import com.ecommerce.cart.client.CatalogServiceClient;
import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.model.Cart;
import com.ecommerce.cart.model.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CatalogServiceClient catalogServiceClient;

    public CartResponse getCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));
        return mapToResponse(cart);
    }

    public CartResponse addItem(String userId, AddToCartRequest request) {
        Map<String, Object> product = catalogServiceClient.getProduct(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));

        List<CartItem> items = cart.getItems();
        if (items == null) {
            items = new ArrayList<>();
            cart.setItems(items);
        }

        int existingQuantity = items.stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst()
                .map(CartItem::getQuantity)
                .orElse(0);

        int totalQuantity = existingQuantity + request.getQuantity();
        if (!catalogServiceClient.checkStock(request.getProductId(), totalQuantity)) {
            throw new RuntimeException("Insufficient stock for product: " + request.getProductId());
        }

        Double catalogPrice = product.get("price") instanceof Number
                ? ((Number) product.get("price")).doubleValue()
                : request.getPrice();
        String catalogName = product.get("name") != null
                ? product.get("name").toString()
                : request.getProductName();

        Optional<CartItem> existingItem = items.stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(totalQuantity);
            existingItem.get().setPrice(catalogPrice);
            existingItem.get().setProductName(catalogName);
        } else {
            CartItem newItem = CartItem.builder()
                    .productId(request.getProductId())
                    .productName(catalogName)
                    .price(catalogPrice)
                    .quantity(request.getQuantity())
                    .build();
            items.add(newItem);
        }

        calculateTotalAmount(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);
        log.info("Item added to cart for user: {}", userId);
        return mapToResponse(savedCart);
    }

    public CartResponse updateItem(String userId, String productId, int quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        List<CartItem> items = cart.getItems();
        if (items == null) {
            throw new RuntimeException("Cart is empty");
        }

        if (quantity == 0) {
            items.removeIf(item -> item.getProductId().equals(productId));
        } else {
            CartItem item = items.stream()
                    .filter(i -> i.getProductId().equals(productId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Item not found in cart: " + productId));
            item.setQuantity(quantity);
        }

        calculateTotalAmount(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);
        log.info("Cart item updated for user: {}, productId: {}", userId, productId);
        return mapToResponse(savedCart);
    }

    public CartResponse removeItem(String userId, String productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        List<CartItem> items = cart.getItems();
        if (items != null) {
            items.removeIf(item -> item.getProductId().equals(productId));
        }

        calculateTotalAmount(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);
        log.info("Item removed from cart for user: {}, productId: {}", userId, productId);
        return mapToResponse(savedCart);
    }

    public void clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        cart.setItems(new ArrayList<>());
        cart.setTotalAmount(0.0);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        log.info("Cart cleared for user: {}", userId);
    }

    private Cart createEmptyCart(String userId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .totalAmount(0.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return cartRepository.save(cart);
    }

    private void calculateTotalAmount(Cart cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            cart.setTotalAmount(0.0);
            return;
        }
        double total = cart.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        cart.setTotalAmount(Math.round(total * 100.0) / 100.0);
    }

    private CartResponse mapToResponse(Cart cart) {
        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .items(cart.getItems())
                .totalAmount(cart.getTotalAmount())
                .itemCount(cart.getItems() != null ? cart.getItems().size() : 0)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
