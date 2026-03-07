package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.RefundRequest;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody PaymentRequest request) {
        log.info("Processing payment for user: {}, orderId: {}", userId, request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.processPayment(userId, request));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @PathVariable String orderId) {
        log.info("Getting payment for orderId: {}", orderId);
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<PaymentResponse>> getPaymentHistory(
            @RequestHeader("X-User-Id") String userId,
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("Getting payment history for user: {}", userId);
        return ResponseEntity.ok(paymentService.getPaymentHistory(userId, pageable));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String paymentId,
            @RequestBody RefundRequest request) {
        log.info("Refunding payment: {} for user: {}", paymentId, userId);
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, userId, request));
    }
}
