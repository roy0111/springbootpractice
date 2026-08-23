package com.learn.restapi.service;

import com.learn.restapi.exception.ResourceNotFoundException;
import com.learn.restapi.model.Product;
import com.learn.restapi.repository.ProductRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Reactive service layer for Product business logic.
 *
 * <p>All methods return Project Reactor types:
 * <ul>
 *   <li>{@link Mono} — 0 or 1 item (think Optional, but async)</li>
 *   <li>{@link Flux} — 0 to N items (think Stream, but async)</li>
 * </ul>
 *
 * <p>Key reactive operators used:
 * <ul>
 *   <li>{@code flatMap()} — async transform (returns Mono/Flux inside a Mono/Flux)</li>
 *   <li>{@code map()} — synchronous transform (returns a plain value)</li>
 *   <li>{@code switchIfEmpty()} — fallback when the upstream is empty</li>
 *   <li>{@code filter()} — conditionally pass items downstream</li>
 *   <li>{@code thenReturn()} — ignore the upstream signal, emit a fixed value</li>
 * </ul>
 *
 * <p>IMPORTANT: Nothing runs until a subscriber subscribes.
 * The WebFlux framework subscribes when the controller method is called.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ── READ operations ─────────────────────────────────────────────────────

    /**
     * Returns all products as a non-blocking stream.
     */
    public Flux<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Returns a product by ID, or signals {@link ResourceNotFoundException}
     * downstream if not found.
     *
     * <p>{@code switchIfEmpty} replaces an empty Mono with an error signal —
     * equivalent to "if null → throw 404".
     */
    public Mono<Product> getProductById(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Product", "id", id)));
    }

    /**
     * Filters products by category.
     */
    public Flux<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category);
    }

    /**
     * Searches products by name keyword.
     */
    public Flux<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }

    // ── WRITE operations ────────────────────────────────────────────────────

    /**
     * Creates a new product.
     *
     * <p>Pipeline:
     * <ol>
     *   <li>Check if a product with the same name exists (async)</li>
     *   <li>{@code flatMap}: if exists → emit error; otherwise save</li>
     * </ol>
     */
    public Mono<Product> createProduct(Product product) {
        return productRepository.existsByNameIgnoreCase(product.getName())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new IllegalArgumentException(
                                "A product with the name '" + product.getName() + "' already exists."));
                    }
                    product.setCreatedAt(LocalDateTime.now());
                    product.setUpdatedAt(LocalDateTime.now());
                    return productRepository.save(product);
                });
    }

    /**
     * Updates an existing product.
     *
     * <p>Pipeline:
     * <ol>
     *   <li>Find the product by ID (404 if missing)</li>
     *   <li>{@code flatMap}: apply field updates on the retrieved entity, then save</li>
     * </ol>
     */
    public Mono<Product> updateProduct(Long id, Product updatedProduct) {
        return getProductById(id)               // Mono<Product> — throws 404 if not found
                .flatMap(existing -> {
                    existing.setName(updatedProduct.getName());
                    existing.setDescription(updatedProduct.getDescription());
                    existing.setPrice(updatedProduct.getPrice());
                    existing.setCategory(updatedProduct.getCategory());
                    existing.setStockQuantity(updatedProduct.getStockQuantity());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return productRepository.save(existing);
                });
    }

    /**
     * Deletes a product by ID.
     *
     * <p>{@code flatMap(productRepository::delete)} returns {@code Mono<Void>}.
     * We first verify the product exists so we can emit a 404 if absent.
     */
    public Mono<Void> deleteProduct(Long id) {
        return getProductById(id)
                .flatMap(productRepository::delete);
    }
}
