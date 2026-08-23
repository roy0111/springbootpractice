package com.learn.restapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * R2DBC entity representing a Product in the catalog.
 *
 * <p>Key differences from a JPA entity:
 * <ul>
 *   <li>{@code @Id} comes from {@code org.springframework.data.annotation} — NOT javax/jakarta.persistence</li>
 *   <li>{@code @Table} comes from {@code org.springframework.data.relational.core.mapping}</li>
 *   <li>No {@code @Entity}, {@code @Column(nullable=…)}, or {@code @PrePersist} — R2DBC is lighter-weight</li>
 *   <li>R2DBC uses {@code schema.sql} for DDL; Hibernate auto-DDL is not available</li>
 *   <li>camelCase fields are automatically mapped to snake_case columns (NamingStrategy)</li>
 * </ul>
 *
 * <p>Bean Validation annotations ({@code @NotBlank}, {@code @DecimalMin} etc.) still work
 * with WebFlux — they are triggered by {@code @Valid} on the {@code @RequestBody}.
 */
@Table("products")
@Schema(description = "Product entity in the catalog")
public class Product {

    @Id
    @Schema(description = "Auto-generated product ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Product name must not be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "Name of the product", example = "Wireless Headphones", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Optional product description", example = "Noise-cancelling over-ear headphones")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Schema(description = "Product price in USD", example = "89.99", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @NotBlank(message = "Category must not be blank")
    @Schema(description = "Product category", example = "Electronics", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column("stock_quantity")   // explicit mapping for clarity (camelCase → snake_case is automatic)
    @Schema(description = "Available stock units", example = "50")
    private int stockQuantity = 0;

    @Column("created_at")
    @Schema(description = "Timestamp when the product was created", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Column("updated_at")
    @Schema(description = "Timestamp when the product was last updated", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    // ── Constructors ────────────────────────────────────────────────────────

    public Product() {}

    public Product(String name, String description, BigDecimal price,
                   String category, int stockQuantity) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.stockQuantity = stockQuantity;
    }

    // ── Getters & Setters ───────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', price=" + price +
               ", category='" + category + "', stock=" + stockQuantity + "}";
    }
}
