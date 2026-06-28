package com.example.myapp.order.domain.model;

import lombok.Builder;

/**
 * Value Object representing credit card details.
 * Immutability is guaranteed by Java record declaration.
 */
@Builder
public record CardDetails(
    String cardNumber,
    String expirationMonth,
    String expirationYear,
    String cvv,
    String cardholderName
) {
    public CardDetails {
        // Enforce basic syntactic checks for safety
        if (cardNumber == null || cardNumber.isBlank()) {
            throw new IllegalArgumentException("Card number must not be blank.");
        }
        if (expirationMonth == null || expirationMonth.isBlank()) {
            throw new IllegalArgumentException("Expiration month must not be blank.");
        }
        if (expirationYear == null || expirationYear.isBlank()) {
            throw new IllegalArgumentException("Expiration year must not be blank.");
        }
        if (cvv == null || cvv.isBlank()) {
            throw new IllegalArgumentException("CVV must not be blank.");
        }
        if (cardholderName == null || cardholderName.isBlank()) {
            throw new IllegalArgumentException("Cardholder name must not be blank.");
        }
    }
}
