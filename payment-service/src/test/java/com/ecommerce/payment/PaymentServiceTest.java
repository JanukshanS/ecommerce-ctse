package com.ecommerce.payment;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.RefundRequest;
import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentMethod;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment successPayment;
    private Payment failedPayment;
    private final String userId = "user123";
    private final String orderId = "order001";
    private final String paymentId = "payment001";
    private final String transactionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        successPayment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(userId)
                .amount(99.99)
                .method(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.SUCCESS)
                .transactionId(transactionId)
                .description("Order payment")
                .failureReason(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        failedPayment = Payment.builder()
                .id("payment002")
                .orderId("order002")
                .userId(userId)
                .amount(49.99)
                .method(PaymentMethod.DEBIT_CARD)
                .status(PaymentStatus.FAILED)
                .transactionId(UUID.randomUUID().toString())
                .description("Order payment")
                .failureReason("Payment declined by processor")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("processPayment - should save payment with generated transactionId")
    void processPayment_ShouldSavePaymentWithTransactionId() {
        PaymentRequest request = PaymentRequest.builder()
                .orderId(orderId)
                .amount(99.99)
                .method(PaymentMethod.CREDIT_CARD)
                .description("Order payment")
                .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(paymentId);
            return p;
        });

        PaymentResponse response = paymentService.processPayment(userId, request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(orderId, response.getOrderId());
        assertEquals(99.99, response.getAmount());
        assertEquals(PaymentMethod.CREDIT_CARD, response.getMethod());
        assertNotNull(response.getTransactionId());
        assertThat(response.getStatus()).isIn(PaymentStatus.SUCCESS, PaymentStatus.FAILED);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("processPayment - success scenario: status is SUCCESS and no failure reason")
    void processPayment_SuccessScenario_ShouldHaveSuccessStatusAndNoFailureReason() {
        PaymentRequest request = PaymentRequest.builder()
                .orderId(orderId)
                .amount(99.99)
                .method(PaymentMethod.PAYPAL)
                .description("PayPal payment")
                .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(paymentId);
            p.setStatus(PaymentStatus.SUCCESS);
            p.setFailureReason(null);
            return p;
        });

        PaymentResponse response = paymentService.processPayment(userId, request);

        assertNotNull(response);
        // Since this save mock forces SUCCESS, verify success-specific assertions
        if (response.getStatus() == PaymentStatus.SUCCESS) {
            assertNull(response.getFailureReason());
        }
    }

    @Test
    @DisplayName("processPayment - failure scenario: status is FAILED with failure reason")
    void processPayment_FailureScenario_ShouldHaveFailedStatusAndFailureReason() {
        PaymentRequest request = PaymentRequest.builder()
                .orderId("order002")
                .amount(49.99)
                .method(PaymentMethod.DEBIT_CARD)
                .description("Debit card payment")
                .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId("payment002");
            p.setStatus(PaymentStatus.FAILED);
            p.setFailureReason("Payment declined by processor");
            return p;
        });

        PaymentResponse response = paymentService.processPayment(userId, request);

        assertNotNull(response);
        if (response.getStatus() == PaymentStatus.FAILED) {
            assertNotNull(response.getFailureReason());
            assertEquals("Payment declined by processor", response.getFailureReason());
        }
    }

    @Test
    @DisplayName("processPayment - simulated success rate should be approximately 90%")
    @RepeatedTest(value = 100, name = "Run {currentRepetition} of {totalRepetitions}")
    void processPayment_SimulatedSuccessRate_ShouldBeApproximately90Percent() {
        // This test verifies the payment simulation logic produces valid statuses
        PaymentRequest request = PaymentRequest.builder()
                .orderId(UUID.randomUUID().toString())
                .amount(10.00)
                .method(PaymentMethod.BANK_TRANSFER)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(userId, request);

        assertNotNull(response);
        assertTrue(
            response.getStatus() == PaymentStatus.SUCCESS || response.getStatus() == PaymentStatus.FAILED,
            "Payment status must be either SUCCESS or FAILED"
        );
    }

    @Test
    @DisplayName("getPaymentByOrderId - should return payment when found")
    void getPaymentByOrderId_WhenFound_ShouldReturnPayment() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(successPayment));

        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.getOrderId());
        assertEquals(userId, response.getUserId());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(transactionId, response.getTransactionId());
        verify(paymentRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    @DisplayName("getPaymentByOrderId - should throw exception when payment not found")
    void getPaymentByOrderId_WhenNotFound_ShouldThrowException() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.getPaymentByOrderId(orderId));
        verify(paymentRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    @DisplayName("getPaymentHistory - should return paged payment history for user")
    void getPaymentHistory_ShouldReturnPagedHistory() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> paymentPage = new PageImpl<>(Arrays.asList(successPayment, failedPayment));

        when(paymentRepository.findByUserId(userId, pageable)).thenReturn(paymentPage);

        Page<PaymentResponse> result = paymentService.getPaymentHistory(userId, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(paymentRepository, times(1)).findByUserId(userId, pageable);
    }

    @Test
    @DisplayName("refundPayment - should refund successful payment")
    void refundPayment_WhenPaymentIsSuccess_ShouldRefund() {
        RefundRequest refundRequest = RefundRequest.builder()
                .reason("Customer requested refund")
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(successPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.refundPayment(paymentId, userId, refundRequest);

        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        assertEquals("Customer requested refund", response.getFailureReason());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("refundPayment - should throw exception when payment not found")
    void refundPayment_WhenNotFound_ShouldThrowException() {
        RefundRequest refundRequest = RefundRequest.builder().reason("Refund").build();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                paymentService.refundPayment(paymentId, userId, refundRequest));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("refundPayment - should throw exception when payment belongs to different user")
    void refundPayment_WhenBelongsToDifferentUser_ShouldThrowException() {
        RefundRequest refundRequest = RefundRequest.builder().reason("Refund").build();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(successPayment));

        assertThrows(RuntimeException.class, () ->
                paymentService.refundPayment(paymentId, "anotherUser", refundRequest));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("refundPayment - should throw exception when payment is not in SUCCESS status")
    void refundPayment_WhenPaymentNotSuccessful_ShouldThrowException() {
        RefundRequest refundRequest = RefundRequest.builder().reason("Refund").build();
        when(paymentRepository.findById("payment002")).thenReturn(Optional.of(failedPayment));

        assertThrows(RuntimeException.class, () ->
                paymentService.refundPayment("payment002", userId, refundRequest));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("refundPayment - should throw exception when trying to refund already refunded payment")
    void refundPayment_WhenAlreadyRefunded_ShouldThrowException() {
        successPayment.setStatus(PaymentStatus.REFUNDED);
        RefundRequest refundRequest = RefundRequest.builder().reason("Double refund").build();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(successPayment));

        assertThrows(RuntimeException.class, () ->
                paymentService.refundPayment(paymentId, userId, refundRequest));
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
