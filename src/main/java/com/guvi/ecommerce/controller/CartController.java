package com.guvi.ecommerce.controller;

import com.guvi.ecommerce.dto.CartItemRequest;
import com.guvi.ecommerce.model.Cart;
import com.guvi.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cart")
@RestController
@RequestMapping("/cart")
@SecurityRequirement(name = "Bearer Auth")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "View cart")
    @GetMapping
    public ResponseEntity<Cart> getCart(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(cartService.getCart(user.getUsername()));
    }

    @Operation(summary = "Add item to cart")
    @PostMapping("/items")
    public ResponseEntity<Cart> addItem(@AuthenticationPrincipal UserDetails user,
                                        @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(user.getUsername(), request));
    }

    @Operation(summary = "Update item quantity")
    @PutMapping("/items/{productId}")
    public ResponseEntity<Cart> updateItem(@AuthenticationPrincipal UserDetails user,
                                           @PathVariable String productId,
                                           @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateItem(user.getUsername(), productId, quantity));
    }

    @Operation(summary = "Remove item from cart")
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Cart> removeItem(@AuthenticationPrincipal UserDetails user,
                                           @PathVariable String productId) {
        return ResponseEntity.ok(cartService.removeItem(user.getUsername(), productId));
    }
}
