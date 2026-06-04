package com.ecommerce.ecommercebackend.exception;

/**
 * Thrown when a request is semantically invalid (e.g. checking out an empty
 * cart, or cancelling an order that can no longer be cancelled).
 * Mapped to HTTP 400 by {@code GlobalExceptionHandler}.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
