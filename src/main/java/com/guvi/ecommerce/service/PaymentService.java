package com.guvi.ecommerce.service;

import com.guvi.ecommerce.exception.ResourceNotFoundException;
import com.guvi.ecommerce.model.Order;
import com.guvi.ecommerce.model.Payment;
import com.guvi.ecommerce.repo.OrderRepository;
import com.guvi.ecommerce.repo.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    public Payment processPayment(String orderId, String userId) {
        // Idempotency: don't process twice
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new RuntimeException("Payment already processed for this order");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found");
        }

        // Simulate payment: 80% success
        boolean success = new Random().nextInt(10) < 8;

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(success ? "SUCCESS" : "FAILED");
        payment.setProcessedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        // Update order status
        order.setStatus(success ? "CONFIRMED" : "CANCELLED");
        orderRepository.save(order);

        if (success) {
            log.info("Payment SUCCESS for order: {}", orderId);
        } else {
            log.warn("Payment FAILED for order: {}", orderId);
        }

        return payment;
    }
}
