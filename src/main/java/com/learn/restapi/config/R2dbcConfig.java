package com.learn.restapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

/**
 * R2DBC configuration.
 *
 * <p>{@code @EnableR2dbcAuditing} activates Spring Data's auditing support for R2DBC,
 * enabling the {@code @CreatedDate} and {@code @LastModifiedDate} annotations
 * on entity fields to be automatically populated.
 *
 * <p>Note: For auditing to work on insert vs. update, entities implement
 * {@link org.springframework.data.domain.Persistable} or use a {@code @Version} field.
 * In this project we set timestamps manually in the service layer for simplicity.
 */
@Configuration
@EnableR2dbcAuditing
public class R2dbcConfig {
    // Spring Boot auto-configures the R2dbcEntityTemplate and connection pool.
    // This class exists solely to enable auditing.
}
