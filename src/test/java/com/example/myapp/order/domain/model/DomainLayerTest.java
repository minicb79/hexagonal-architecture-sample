package com.example.myapp.order.domain.model;

import com.example.myapp.order.domain.exception.CardNotFoundException;
import com.example.myapp.order.domain.exception.PaymentSessionFailedException;
import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DomainLayerTest {

    @Test
    void shouldCreateCardDetailsSuccessfullyWhenParametersAreValid() {
        CardDetails card = new CardDetails(
            "1234567890123456", "12", "2030", "123", "John Doe"
        );

        assertEquals("1234567890123456", card.cardNumber());
        assertEquals("12", card.expirationMonth());
        assertEquals("2030", card.expirationYear());
        assertEquals("123", card.cvv());
        assertEquals("John Doe", card.cardholderName());
    }

    @Test
    void shouldThrowExceptionWhenCardDetailsContainBlankOrNullValues() {
        assertThrows(IllegalArgumentException.class, () -> 
            new CardDetails(null, "12", "2030", "123", "John Doe")
        );
        assertThrows(IllegalArgumentException.class, () -> 
            new CardDetails("1234567890123456", " ", "2030", "123", "John Doe")
        );
    }

    @Test
    void cardDetailsShouldHaveValueObjectEqualityBehavior() {
        CardDetails card1 = new CardDetails("1234567890123456", "12", "2030", "123", "John Doe");
        CardDetails card2 = new CardDetails("1234567890123456", "12", "2030", "123", "John Doe");
        CardDetails card3 = new CardDetails("9999999999999999", "12", "2030", "123", "John Doe");

        assertEquals(card1, card2); // Equal values mean equal objects
        assertNotEquals(card1, card3);
        assertEquals(card1.hashCode(), card2.hashCode());
    }

    @Test
    void shouldCreatePaymentSessionSuccessfullyWhenParametersAreValid() {
        Instant expiry = Instant.now().plusSeconds(3600);
        PaymentSession session = new PaymentSession("session-id-123", "APPROVED", expiry);

        assertEquals("session-id-123", session.sessionId());
        assertEquals("APPROVED", session.status());
        assertEquals(expiry, session.expiresAt());
    }

    @Test
    void shouldThrowExceptionWhenPaymentSessionContainsNullOrBlankValues() {
        Instant expiry = Instant.now().plusSeconds(3600);
        assertThrows(IllegalArgumentException.class, () -> 
            new PaymentSession("", "APPROVED", expiry)
        );
        assertThrows(IllegalArgumentException.class, () -> 
            new PaymentSession("id", null, expiry)
        );
        assertThrows(IllegalArgumentException.class, () -> 
            new PaymentSession("id", "APPROVED", null)
        );
    }

    @Test
    void domainExceptionsShouldPreserveMessageAndCause() {
        Exception cause = new RuntimeException("Http client failure");
        CardNotFoundException cardEx = new CardNotFoundException("Card lookup failed", cause);
        PaymentSessionFailedException payEx = new PaymentSessionFailedException("Payment failed", cause);

        assertEquals("Card lookup failed", cardEx.getMessage());
        assertEquals(cause, cardEx.getCause());

        assertEquals("Payment failed", payEx.getMessage());
        assertEquals(cause, payEx.getCause());
    }
}
