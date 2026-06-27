package com.example.myapp.order.domain.exception;

/**
 * Domain Exception thrown when a requested saved card cannot be found.
 */
public class CardNotFoundException extends RuntimeException {
    
    public CardNotFoundException(String message) {
        super(message);
    }

    public CardNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
