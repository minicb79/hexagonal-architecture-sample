package com.example.myapp.order.domain.model;

import com.example.myapp.order.domain.exception.CardNotFoundException;
import com.example.myapp.order.domain.exception.PaymentSessionFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DomainLayerTest {

    @Test
    void shouldCreateCardDetailsSuccessfullyWhenParametersAreValid() {
        CardDetails card = CardDetails.builder()
                .cardNumber("1234567890123456")
                .expirationMonth("12")
                .expirationYear("2030")
                .cvv("123")
                .cardholderName("John Doe")
                .build();

        assertEquals("1234567890123456", card.cardNumber());
        assertEquals("12", card.expirationMonth());
        assertEquals("2030", card.expirationYear());
        assertEquals("123", card.cvv());
        assertEquals("John Doe", card.cardholderName());
    }

    @ParameterizedTest
    @CsvSource({
        ", 12, 2030, 123, John Doe",
        "' ', 12, 2030, 123, John Doe",
        "1234567890123456, , 2030, 123, John Doe",
        "1234567890123456, ' ', 2030, 123, John Doe",
        "1234567890123456, 12, , 123, John Doe",
        "1234567890123456, 12, ' ', 123, John Doe",
        "1234567890123456, 12, 2030, , John Doe",
        "1234567890123456, 12, 2030, ' ', John Doe",
        "1234567890123456, 12, 2030, 123, ",
        "1234567890123456, 12, 2030, 123, ' '"
    })
    void shouldThrowExceptionWhenCardDetailsContainBlankOrNullValues(
            String number, String month, String year, String cvv, String holder) {
        assertThrows(IllegalArgumentException.class, () ->
            CardDetails.builder()
                    .cardNumber(number)
                    .expirationMonth(month)
                    .expirationYear(year)
                    .cvv(cvv)
                    .cardholderName(holder)
                    .build()
        );
    }

    @Test
    void cardDetailsShouldHaveValueObjectEqualityBehavior() {
        CardDetails card1 = CardDetails.builder()
                .cardNumber("1234567890123456")
                .expirationMonth("12")
                .expirationYear("2030")
                .cvv("123")
                .cardholderName("John Doe")
                .build();
        CardDetails card2 = CardDetails.builder()
                .cardNumber("1234567890123456")
                .expirationMonth("12")
                .expirationYear("2030")
                .cvv("123")
                .cardholderName("John Doe")
                .build();
        CardDetails card3 = CardDetails.builder()
                .cardNumber("9999999999999999")
                .expirationMonth("12")
                .expirationYear("2030")
                .cvv("123")
                .cardholderName("John Doe")
                .build();

        assertEquals(card1, card2); // Equal values mean equal objects
        assertNotEquals(card1, card3);
        assertEquals(card1.hashCode(), card2.hashCode());
    }

    @Test
    void shouldCreatePaymentSessionSuccessfullyWhenParametersAreValid() {
        Instant expiry = Instant.now().plusSeconds(3600);
        PaymentSession session = PaymentSession.builder()
                .sessionId("session-id-123")
                .status("APPROVED")
                .expiresAt(expiry)
                .build();

        assertEquals("session-id-123", session.sessionId());
        assertEquals("APPROVED", session.status());
        assertEquals(expiry, session.expiresAt());
    }

    @ParameterizedTest
    @CsvSource({
        "'', APPROVED, true",
        "' ', APPROVED, true",
        "id, , true",
        "id, APPROVED, false"
    })
    void shouldThrowExceptionWhenPaymentSessionContainsNullOrBlankValues(
            String id, String status, boolean hasExpiry) {
        Instant expiry = hasExpiry ? Instant.now() : null;
        assertThrows(IllegalArgumentException.class, () ->
            PaymentSession.builder()
                    .sessionId(id)
                    .status(status)
                    .expiresAt(expiry)
                    .build()
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
