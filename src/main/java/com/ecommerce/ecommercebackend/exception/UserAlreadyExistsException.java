package com.ecommerce.ecommercebackend.exception;

/**
 * Thrown when a registration attempt is made with a username or email
 * that already exists in the system.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
