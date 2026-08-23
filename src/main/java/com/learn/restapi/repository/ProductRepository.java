package com.learn.restapi.repository;

import com.learn.restapi.model.Product;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive Spring Data R2DBC repository for {@link Product}.
 *
 * <p>By extending {@link ReactiveCrudRepository} we automatically get:
 * <ul>
 *   <li>{@code save()} → {@code Mono<Product>}</li>
 *   <li>{@code findById()} → {@code Mono<Product>}</li>
 *   <li>{@code findAll()} → {@code Flux<Product>}</li>
 *   <li>{@code deleteById()} → {@code Mono<Void>}</li>
 *   <li>… and more, all non-blocking</li>
 * </ul>
 *
 * <p>Custom query methods are derived from the method name — Spring Data generates
 * the SQL at startup. Return types MUST be {@code Flux<T>} (many) or {@code Mono<T>} (one/zero).
 */
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {

    /**
     * Returns all products in the given category (case-insensitive).
     * Generated query: SELECT * FROM products WHERE LOWER(category) = LOWER(:category)
     */
    Flux<Product> findByCategoryIgnoreCase(String category);

    /**
     * Returns products whose name contains the keyword (case-insensitive).
     */
    Flux<Product> findByNameContainingIgnoreCase(String keyword);

    /**
     * Checks if a product with the given name already exists (case-insensitive).
     * Returns {@code Mono<Boolean>} — subscribe to get the result reactively.
     */
    Mono<Boolean> existsByNameIgnoreCase(String name);
}
