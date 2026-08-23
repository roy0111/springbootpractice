package com.learn.restapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Spring Boot REST API + Actuator learning application.
 *
 * <p>Key features demonstrated:
 * <ul>
 *   <li>RESTful CRUD API for Product Catalog</li>
 *   <li>Spring Data JPA with H2 in-memory database</li>
 *   <li>Bean Validation with @Valid</li>
 *   <li>Global exception handling with @ControllerAdvice</li>
 *   <li>Spring Boot Actuator — health, metrics, info, env, mappings</li>
 * </ul>
 */
@SpringBootApplication
public class RestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestApiApplication.class, args);
    }
}
