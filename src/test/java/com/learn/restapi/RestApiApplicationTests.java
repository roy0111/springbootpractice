package com.learn.restapi;

import com.learn.restapi.exception.ResourceNotFoundException;
import com.learn.restapi.model.Product;
import com.learn.restapi.repository.ProductRepository;
import com.learn.restapi.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reactive integration tests for the Spring Boot application.
 *
 * <p>{@code @SpringBootTest} starts the full application context with R2DBC + H2.
 *
 * <p>{@link StepVerifier} is the Reactor testing utility:
 * <ul>
 *   <li>{@code expectNext()} / {@code expectNextMatches()} — assert emitted items</li>
 *   <li>{@code expectError()} — assert error signals</li>
 *   <li>{@code verifyComplete()} — subscribe and assert the stream completes normally</li>
 *   <li>{@code verifyError()} — subscribe and assert the stream ends with an error</li>
 * </ul>
 */
@SpringBootTest
class RestApiApplicationTests {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanUp() {
        // Clear DB before each test for isolation
        productRepository.deleteAll().block();
    }

    // ── Context ─────────────────────────────────────────────────────────────

    @Test
    void contextLoads() {
        // Verifies the full Spring context starts without errors
        assert productService != null;
        assert productRepository != null;
    }

    // ── Create ──────────────────────────────────────────────────────────────

    @Test
    void shouldCreateAndRetrieveProduct() {
        Product p = new Product("Laptop", "Gaming laptop",
                new BigDecimal("1299.99"), "Electronics", 10);

        StepVerifier.create(
                productService.createProduct(p)
        )
        .expectNextMatches(saved ->
                saved.getId() != null &&
                saved.getName().equals("Laptop") &&
                saved.getCreatedAt() != null
        )
        .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateName() {
        Product p1 = new Product("Widget", "First", new BigDecimal("10.00"), "Tools", 5);
        Product p2 = new Product("Widget", "Duplicate", new BigDecimal("15.00"), "Tools", 3);

        StepVerifier.create(
                productService.createProduct(p1)
                        .then(productService.createProduct(p2))
        )
        .expectError(IllegalArgumentException.class)
        .verify();
    }

    // ── Read ────────────────────────────────────────────────────────────────

    @Test
    void shouldGetAllProducts() {
        Product p1 = new Product("A", "desc", new BigDecimal("1.00"), "Cat", 1);
        Product p2 = new Product("B", "desc", new BigDecimal("2.00"), "Cat", 2);

        StepVerifier.create(
                productService.createProduct(p1)
                        .then(productService.createProduct(p2))
                        .thenMany(productService.getAllProducts())
        )
        .expectNextCount(2)
        .verifyComplete();
    }

    @Test
    void shouldThrow404ForMissingProduct() {
        StepVerifier.create(
                productService.getProductById(999L)
        )
        .expectError(ResourceNotFoundException.class)
        .verify();
    }

    @Test
    void shouldFilterByCategory() {
        Flux<Product> setup = Flux.just(
                new Product("Phone",  "desc", new BigDecimal("500.00"), "Electronics", 5),
                new Product("Hammer", "desc", new BigDecimal("20.00"),  "Tools",       10)
        ).flatMap(productService::createProduct);

        StepVerifier.create(
                setup.thenMany(productService.getProductsByCategory("Electronics"))
        )
        .expectNextMatches(p -> p.getCategory().equalsIgnoreCase("Electronics"))
        .verifyComplete();
    }

    // ── Update ──────────────────────────────────────────────────────────────

    @Test
    void shouldUpdateProduct() {
        Product original = new Product("Old Name", "desc",
                new BigDecimal("10.00"), "Misc", 1);
        Product updated  = new Product("New Name", "desc",
                new BigDecimal("20.00"), "Misc", 2);

        StepVerifier.create(
                productService.createProduct(original)
                        .flatMap(saved -> productService.updateProduct(saved.getId(), updated))
        )
        .expectNextMatches(p ->
                p.getName().equals("New Name") &&
                p.getPrice().compareTo(new BigDecimal("20.00")) == 0
        )
        .verifyComplete();
    }

    // ── Delete ──────────────────────────────────────────────────────────────

    @Test
    void shouldDeleteProduct() {
        Product p = new Product("ToDelete", "desc", new BigDecimal("5.00"), "Misc", 1);

        StepVerifier.create(
                productService.createProduct(p)
                        .flatMap(saved -> productService.deleteProduct(saved.getId()))
                        .then(productRepository.count())
        )
        .expectNext(0L)
        .verifyComplete();
    }
}
