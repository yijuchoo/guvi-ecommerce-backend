package com.guvi.ecommerce.service;

import com.guvi.ecommerce.dto.CartItemRequest;
import com.guvi.ecommerce.exception.InsufficientStockException;
import com.guvi.ecommerce.exception.ResourceNotFoundException;
import com.guvi.ecommerce.model.Cart;
import com.guvi.ecommerce.model.CartItem;
import com.guvi.ecommerce.model.Product;
import com.guvi.ecommerce.repo.CartRepository;
import com.guvi.ecommerce.repo.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public Cart getCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(userId)));
    }

    public Cart addItem(String userId, CartItemRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException("Not enough stock. Available: " + product.getStockQuantity());
        }

        Cart cart = getCart(userId);
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existing.isPresent()) {
            int newQty = existing.get().getQuantity() + request.getQuantity();
            if (newQty > product.getStockQuantity()) {
                throw new InsufficientStockException("Total quantity exceeds stock");
            }
            existing.get().setQuantity(newQty);
        } else {
            cart.getItems().add(new CartItem(
                    product.getId(),
                    product.getName(),
                    request.getQuantity(),
                    product.getPrice()
            ));
        }
        return cartRepository.save(cart);
    }

    public Cart updateItem(String userId, String productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException("Not enough stock");
        }

        Cart cart = getCart(userId);
        cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not in cart"))
                .setQuantity(quantity);

        return cartRepository.save(cart);
    }

    public Cart removeItem(String userId, String productId) {
        Cart cart = getCart(userId);
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        return cartRepository.save(cart);
    }
}
