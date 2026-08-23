package com.learn.restapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger / OpenAPI configuration.
 *
 * <p>Springdoc auto-scans all {@code @RestController} classes and generates
 * the OpenAPI specification. This bean enriches it with metadata.
 *
 * <p>After startup:
 * <ul>
 *   <li>Swagger UI   → <a href="http://localhost:8080/swagger-ui.html">http://localhost:8080/swagger-ui.html</a></li>
 *   <li>OpenAPI JSON → <a href="http://localhost:8080/v3/api-docs">http://localhost:8080/v3/api-docs</a></li>
 * </ul>
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI productCatalogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Product Catalog REST API")
                        .description("""
                                Reactive REST API built with **Spring WebFlux** + **R2DBC** (H2).
                                
                                ### Key Features
                                - Full CRUD for Products
                                - Non-blocking reactive stack (Project Reactor)
                                - Bean Validation with structured error responses
                                - Spring Boot Actuator for operational monitoring
                                
                                ### Try it out!
                                Use the **Try it out** button on each endpoint to send live requests.
                                """)
                        .version("v2.0.0")
                        .contact(new Contact()
                                .name("Spring Boot Learner")
                                .email("learner@example.com")
                                .url("https://github.com/spring-projects/spring-boot"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")));
    }
}
