package com.learn.restapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (e.g. Product) is not found in the database.
 *
 * <p>{@code @ResponseStatus(HttpStatus.NOT_FOUND)} tells Spring to automatically
 * map this exception to an HTTP 404 response when it is not handled elsewhere.
 * However, our {@link GlobalExceptionHandler} takes over for a richer JSON body.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s = '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName()    { return fieldName; }
    public Object getFieldValue()   { return fieldValue; }
}
