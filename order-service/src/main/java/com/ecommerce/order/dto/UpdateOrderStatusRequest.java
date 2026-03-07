package com.ecommerce.order.dto;

import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String paymentId;
}
