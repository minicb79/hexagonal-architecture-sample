package com.example.myapp.order.domain.exception;

/**
 * Domain Exception thrown when payment session generation downstream fails.
 */
public class PaymentSessionFailedException extends RuntimeException {

    public PaymentSessionFailedException(String message) {
        super(message);
    }

    public PaymentSessionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
