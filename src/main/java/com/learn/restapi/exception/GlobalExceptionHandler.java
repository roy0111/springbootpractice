package com.learn.restapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for reactive WebFlux.
 *
 * <p>{@code @RestControllerAdvice} works with both Spring MVC and Spring WebFlux.
 *
 * <p>Key difference from the MVC version:
 * <ul>
 *   <li>In WebFlux, validation failures throw {@link WebExchangeBindException}
 *       (which extends {@code MethodArgumentNotValidException} — same handler works)</li>
 *   <li>{@code WebRequest} is replaced by {@link ServerWebExchange} for path extraction</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Helper: standard error response body ───────────────────────────────

    private Map<String, Object> buildErrorBody(HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);
        body.put("path",      path);
        return body;
    }

    // ── 404 Not Found ──────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex, ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildErrorBody(HttpStatus.NOT_FOUND, ex.getMessage(), path));
    }

    // ── 400 Validation errors (@Valid fails) ───────────────────────────────
    // WebFlux throws WebExchangeBindException (subclass of MethodArgumentNotValidException)

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            WebExchangeBindException ex, ServerWebExchange exchange) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field   = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            fieldErrors.put(field, message);
        });

        String path = exchange.getRequest().getPath().value();
        Map<String, Object> body = buildErrorBody(HttpStatus.BAD_REQUEST, "Validation failed", path);
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    // ── 400 Illegal arguments ──────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();
        return ResponseEntity
                .badRequest()
                .body(buildErrorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), path));
    }

    // ── 500 Catch-all ──────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorBody(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred: " + ex.getMessage(), path));
    }
}
