package com.guvi.ecommerce.service;

import com.guvi.ecommerce.exception.InsufficientStockException;
import com.guvi.ecommerce.exception.ResourceNotFoundException;
import com.guvi.ecommerce.model.*;
import com.guvi.ecommerce.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private OrderService orderService;

    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product("Laptop", "Fast", 999.0, 10, "Electronics");
        product.setId("prod1");

        CartItem item = new CartItem("prod1", "Laptop", 2, 999.0);
        cart = new Cart("user1");
        cart.setItems(new ArrayList<>(List.of(item)));
    }

    @Test
    void placeOrder_WithValidCart_CreatesOrder() {
        when(orderRepository.findByIdempotencyKey("key1")).thenReturn(Optional.empty());
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));
        // Simulate successful stock deduction (returns non-null)
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), eq(Product.class)))
                .thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("order1");
            return o;
        });
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        Order result = orderService.placeOrder("user1", "key1");

        assertNotNull(result);
        assertEquals("PLACED", result.getStatus());
        assertEquals("user1", result.getUserId());
        verify(cartRepository).save(any(Cart.class)); // cart was cleared
    }

    @Test
    void placeOrder_WhenIdempotencyKeyExists_ReturnsExistingOrder() {
        Order existing = new Order();
        existing.setId("existing1");
        existing.setStatus("PLACED");

        when(orderRepository.findByIdempotencyKey("key1")).thenReturn(Optional.of(existing));

        Order result = orderService.placeOrder("user1", "key1");

        assertEquals("existing1", result.getId());
        // New order should NOT be created
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_WithEmptyCart_ThrowsException() {
        cart.setItems(new ArrayList<>());
        when(orderRepository.findByIdempotencyKey("key1")).thenReturn(Optional.empty());
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.placeOrder("user1", "key1"));
    }

    @Test
    void placeOrder_WhenStockInsufficient_ThrowsException() {
        when(orderRepository.findByIdempotencyKey("key1")).thenReturn(Optional.empty());
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));
        // Returning null simulates "no product matched the query" = insufficient stock
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), eq(Product.class)))
                .thenReturn(null);

        assertThrows(InsufficientStockException.class,
                () -> orderService.placeOrder("user1", "key1"));
    }

    @Test
    void getUserOrders_ReturnsOrderList() {
        Order o1 = new Order();
        o1.setId("o1");
        Order o2 = new Order();
        o2.setId("o2");
        when(orderRepository.findByUserId("user1")).thenReturn(List.of(o1, o2));

        List<Order> result = orderService.getUserOrders("user1");

        assertEquals(2, result.size());
    }
}
