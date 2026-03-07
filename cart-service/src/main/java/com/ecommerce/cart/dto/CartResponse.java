package com.ecommerce.cart.dto;

import com.ecommerce.cart.model.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private String cartId;
    private String userId;
    private List<CartItem> items;
    private Double totalAmount;
    private int itemCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
