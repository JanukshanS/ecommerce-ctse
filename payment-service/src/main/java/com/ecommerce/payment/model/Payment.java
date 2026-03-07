package com.ecommerce.payment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    private String orderId;

    private String userId;

    private Double amount;

    private PaymentMethod method;

    private PaymentStatus status;

    private String transactionId;

    private String description;

    private String failureReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
