package com.guvi.ecommerce.service;

import com.guvi.ecommerce.dto.CartItemRequest;
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

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        product = new Product("Laptop", "Fast", 999.0, 10, "Electronics");
        product.setId("prod1");

        cart = new Cart("user1");
        cart.setItems(new ArrayList<>());
    }

    @Test
    void addItem_WhenStockAvailable_AddsToCart() {
        CartItemRequest request = new CartItemRequest();
        request.setProductId("prod1");
        request.setQuantity(2);

        when(productRepository.findById("prod1")).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartService.addItem("user1", request);

        assertEquals(1, result.getItems().size());
        assertEquals(2, result.getItems().get(0).getQuantity());
        assertEquals("Laptop", result.getItems().get(0).getProductName());
    }

    @Test
    void addItem_WhenInsufficientStock_ThrowsException() {
        CartItemRequest request = new CartItemRequest();
        request.setProductId("prod1");
        request.setQuantity(20); // more than available (10)

        when(productRepository.findById("prod1")).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class,
                () -> cartService.addItem("user1", request));
    }

    @Test
    void addItem_WhenProductNotFound_ThrowsException() {
        CartItemRequest request = new CartItemRequest();
        request.setProductId("missing");
        request.setQuantity(1);

        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.addItem("user1", request));
    }

    @Test
    void removeItem_RemovesCorrectItem() {
        CartItem item = new CartItem("prod1", "Laptop", 2, 999.0);
        cart.getItems().add(item);

        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartService.removeItem("user1", "prod1");

        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void getCart_WhenCartDoesNotExist_CreatesNewCart() {
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartService.getCart("user1");

        assertNotNull(result);
        assertEquals("user1", result.getUserId());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }
}
