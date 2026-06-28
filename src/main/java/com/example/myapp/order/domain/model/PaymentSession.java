package com.example.myapp.order.domain.model;

import java.time.Instant;
import lombok.Builder;

/**
 * Domain Entity representing a created payment session.
 */
@Builder
public record PaymentSession(
    String sessionId,
    String status,
    Instant expiresAt
) {
    public PaymentSession {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID must not be blank.");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status must not be blank.");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiration timestamp must not be null.");
        }
    }
}
