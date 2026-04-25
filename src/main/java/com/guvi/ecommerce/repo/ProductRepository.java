package com.guvi.ecommerce.repo;

import com.guvi.ecommerce.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ProductRepository extends MongoRepository<Product, String> {

    // Search by name or category (case-insensitive)
    @Query("{ '$or': [ { 'name': { '$regex': ?0, '$options': 'i' } }, { 'category': { '$regex': ?0, '$options': 'i' }" +
            " } ] }")
    Page<Product> searchByNameOrCategory(String keyword, Pageable pageable);

    Page<Product> findByCategory(String category, Pageable pageable);
}
