package com.guvi.ecommerce.repo;

import com.guvi.ecommerce.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    boolean existsByOrderId(String orderId);
}
