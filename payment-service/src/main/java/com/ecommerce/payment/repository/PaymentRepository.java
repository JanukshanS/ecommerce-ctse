package com.ecommerce.payment.repository;

import com.ecommerce.payment.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> findByOrderId(String orderId);

    Page<Payment> findByUserId(String userId, Pageable pageable);

    Optional<Payment> findByTransactionId(String transactionId);
}
