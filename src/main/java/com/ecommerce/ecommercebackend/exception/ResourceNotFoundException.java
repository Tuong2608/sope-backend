package com.ecommerce.ecommercebackend.exception;

/**
 * Thrown when a requested domain resource (e.g. a product) does not exist.
 * Mapped to HTTP 404 by {@code GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
