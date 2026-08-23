package com.learn.restapi.controller;

import com.learn.restapi.model.Product;
import com.learn.restapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Reactive REST Controller for the Product Catalog API.
 *
 * <p>Base URL: {@code /api/products}
 *
 * <p><b>Reactive return types:</b>
 * <ul>
 *   <li>{@code Flux<Product>}            — stream of 0–N products</li>
 *   <li>{@code Mono<ResponseEntity<T>>}  — async single response with HTTP status control</li>
 *   <li>{@code Mono<Void>}               — async completion signal (used internally)</li>
 * </ul>
 *
 * <p>WebFlux subscribes to each publisher when the HTTP request arrives.
 * No thread is blocked waiting for the database.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "CRUD operations for the Product Catalog")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ── GET /api/products ───────────────────────────────────────────────────

    @Operation(
        summary     = "Get all products",
        description = "Returns a reactive stream of all products. " +
                      "Use `?category=Electronics` to filter by category, " +
                      "or `?search=phone` to search by name keyword."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "List of products (empty array if none exist)",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array     = @ArraySchema(schema = @Schema(implementation = Product.class))
            )
        )
    })
    @GetMapping
    public Flux<Product> getAllProducts(
            @Parameter(description = "Filter by category (case-insensitive)", example = "Electronics")
            @RequestParam(required = false) String category,

            @Parameter(description = "Search by name keyword (case-insensitive)", example = "phone")
            @RequestParam(required = false) String search) {

        if (category != null && !category.isBlank()) {
            return productService.getProductsByCategory(category);
        } else if (search != null && !search.isBlank()) {
            return productService.searchProducts(search);
        }
        return productService.getAllProducts();
    }

    // ── GET /api/products/{id} ──────────────────────────────────────────────

    @Operation(
        summary     = "Get product by ID",
        description = "Returns a single product. Returns 404 if the product does not exist."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "Product found",
            content      = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = Product.class))
        ),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Product>> getProductById(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long id) {

        return productService.getProductById(id)
                .map(ResponseEntity::ok);
        // ResourceNotFoundException bubbles up → GlobalExceptionHandler → 404
    }

    // ── POST /api/products ──────────────────────────────────────────────────

    @Operation(
        summary     = "Create a new product",
        description = "Creates a product and returns it with the generated ID and timestamps. " +
                      "Returns 400 if validation fails or a product with the same name exists."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description  = "Product created successfully",
            content      = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = Product.class))
        ),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate name",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PostMapping
    public Mono<ResponseEntity<Product>> createProduct(
            @Parameter(description = "Product data to create", required = true)
            @Valid @RequestBody Product product) {

        return productService.createProduct(product)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    // ── PUT /api/products/{id} ──────────────────────────────────────────────

    @Operation(
        summary     = "Update an existing product",
        description = "Fully replaces the product's fields. Returns 404 if not found, " +
                      "400 if validation fails."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "Product updated successfully",
            content      = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = Product.class))
        ),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "400", description = "Validation error",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Product>> updateProduct(
            @Parameter(description = "Product ID to update", example = "1", required = true)
            @PathVariable Long id,

            @Parameter(description = "Updated product data", required = true)
            @Valid @RequestBody Product product) {

        return productService.updateProduct(id, product)
                .map(ResponseEntity::ok);
    }

    // ── DELETE /api/products/{id} ───────────────────────────────────────────

    @Operation(
        summary     = "Delete a product",
        description = "Deletes the product with the given ID. Returns 404 if not found."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product deleted successfully",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Map<String, String>>> deleteProduct(
            @Parameter(description = "Product ID to delete", example = "1", required = true)
            @PathVariable Long id) {

        return productService.deleteProduct(id)
                .thenReturn(ResponseEntity.ok(Map.of(
                        "message", "Product with id " + id + " was deleted successfully."
                )));
        // thenReturn() emits the value after the upstream Mono<Void> completes
    }
}
