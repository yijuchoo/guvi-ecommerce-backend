package com.guvi.ecommerce.service;

import com.guvi.ecommerce.exception.InsufficientStockException;
import com.guvi.ecommerce.exception.ResourceNotFoundException;
import com.guvi.ecommerce.model.*;
import com.guvi.ecommerce.repo.CartRepository;
import com.guvi.ecommerce.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final MongoTemplate mongoTemplate;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        MongoTemplate mongoTemplate) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public Order placeOrder(String userId, String idempotencyKey) {
        // Idempotency check
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> createNewOrder(userId, idempotencyKey));
    }

    private Order createNewOrder(String userId, String idempotencyKey) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResourceNotFoundException("Cart is empty");
        }

        // Atomically deduct stock for each item
        for (CartItem item : cart.getItems()) {
            Query query = new Query(
                    Criteria.where("_id").is(item.getProductId())
                            .and("stockQuantity").gte(item.getQuantity())
            );
            Update update = new Update().inc("stockQuantity", -item.getQuantity());
            Product updated = mongoTemplate.findAndModify(query, update, Product.class);
            if (updated == null) {
                throw new InsufficientStockException("Insufficient stock for: " + item.getProductName());
            }
        }

        // Build order
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(i -> new OrderItem(i.getProductId(), i.getProductName(), i.getQuantity(), i.getPrice()))
                .collect(Collectors.toList());

        double total = orderItems.stream()
                .mapToDouble(i -> i.getPriceAtPurchase() * i.getQuantity())
                .sum();

        Order order = new Order();
        order.setUserId(userId);
        order.setItems(orderItems);
        order.setTotalAmount(total);
        order.setStatus("PLACED");
        order.setIdempotencyKey(idempotencyKey);
        order.setCreatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);

        // Clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order placed: {} for user: {}", saved.getId(), userId);
        return saved;
    }

    public Order getOrderById(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found");
        }
        return order;
    }

    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserId(userId);
    }
}
