package com.guvi.ecommerce.service;

import com.guvi.ecommerce.dto.ProductRequest;
import com.guvi.ecommerce.exception.ResourceNotFoundException;
import com.guvi.ecommerce.model.Product;
import com.guvi.ecommerce.repo.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // tells JUnit to use Mockito
class ProductServiceTest {

    @Mock  // creates a FAKE ProductRepository - no real database
    private ProductRepository productRepository;

    @InjectMocks  // creates REAL ProductService and puts the fake repo inside it
    private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
        // runs before EACH test to reset test data
    void setUp() {
        sampleProduct = new Product("Laptop", "Fast laptop", 999.99, 10, "Electronics");
        sampleProduct.setId("prod1");
    }

    // ---- Happy path tests ----

    @Test
    void getProductById_WhenExists_ReturnsProduct() {
        // ARRANGE: tell fake repo what to return when findById is called
        when(productRepository.findById("prod1")).thenReturn(Optional.of(sampleProduct));

        // ACT: call the real service method
        Product result = productService.getProductById("prod1");

        // ASSERT: check the result is correct
        assertNotNull(result);
        assertEquals("Laptop", result.getName());
        assertEquals(999.99, result.getPrice());
        assertEquals("Electronics", result.getCategory());
    }

    @Test
    void createProduct_ValidRequest_SavesAndReturns() {
        ProductRequest request = new ProductRequest();
        request.setName("Phone");
        request.setDescription("Smart phone");
        request.setPrice(599.0);
        request.setStockQuantity(20);
        request.setCategory("Mobile");

        Product savedProduct = new Product("Phone", "Smart phone", 599.0, 20, "Mobile");
        savedProduct.setId("prod2");

        // When save() is called with any Product, return savedProduct
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        Product result = productService.createProduct(request);

        assertEquals("prod2", result.getId());
        assertEquals("Phone", result.getName());
        // Verify save was called exactly once
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void getAllProducts_ReturnsPaginatedResults() {
        Page<Product> mockPage = new PageImpl<>(List.of(sampleProduct));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

        Page<Product> result = productService.getAllProducts(0, 10, "name");

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateProduct_WhenExists_UpdatesFields() {
        ProductRequest request = new ProductRequest();
        request.setName("Updated Laptop");
        request.setDescription("Even faster");
        request.setPrice(1199.0);
        request.setStockQuantity(5);
        request.setCategory("Electronics");

        when(productRepository.findById("prod1")).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.updateProduct("prod1", request);

        assertEquals("Updated Laptop", result.getName());
        assertEquals(1199.0, result.getPrice());
    }

    @Test
    void deleteProduct_WhenExists_DeletesSuccessfully() {
        when(productRepository.findById("prod1")).thenReturn(Optional.of(sampleProduct));
        doNothing().when(productRepository).delete(any(Product.class));

        assertDoesNotThrow(() -> productService.deleteProduct("prod1"));
        verify(productRepository, times(1)).delete(sampleProduct);
    }

    // ---- Error case tests ----

    @Test
    void getProductById_WhenNotFound_ThrowsException() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        // Assert that calling this throws ResourceNotFoundException
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById("missing"));

        assertEquals("Product not found: missing", ex.getMessage());
    }

    @Test
    void deleteProduct_WhenNotFound_ThrowsException() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.deleteProduct("missing"));

        // Verify delete was NEVER called
        verify(productRepository, never()).delete(any());
    }
}
