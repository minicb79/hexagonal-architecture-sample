package com.example.myapp.order.adapter.out.paymentsservice;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.example.myapp.order.domain.exception.PaymentSessionFailedException;
import com.example.myapp.order.domain.model.CardDetails;
import com.example.myapp.order.domain.model.PaymentSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentsServiceAdapterIntegrationTest {

    private WireMockServer wireMockServer;
    private PaymentsServiceAdapter paymentsAdapter;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .build();

        paymentsAdapter = new PaymentsServiceAdapter(webClient);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void paymentsAdapterShouldCreateSessionSuccessfully() {
        // GIVEN
        CardDetails cardDetails = CardDetails.builder()
                .cardNumber("1234567890123456")
                .expirationMonth("11")
                .expirationYear("29")
                .cvv("999")
                .cardholderName("Alice Smith")
                .build();
        wireMockServer.stubFor(post(urlEqualTo("/sessions"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "session_id": "gateway-session-999",
                                  "status": "APPROVED",
                                  "expires_at": "2030-01-01T12:00:00Z"
                                }
                                """)));

        // WHEN
        PaymentSession session = paymentsAdapter.createSession(cardDetails);

        // THEN
        assertNotNull(session);
        assertEquals("gateway-session-999", session.sessionId());
        assertEquals("APPROVED", session.status());
        assertEquals(Instant.parse("2030-01-01T12:00:00Z"), session.expiresAt());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401})
    void paymentsAdapterShouldTranslateDownstreamErrorsToPaymentSessionFailedExceptionAndNotRetry(int statusCode) {
        // GIVEN
        CardDetails cardDetails = CardDetails.builder()
                .cardNumber("1234567890123456")
                .expirationMonth("11")
                .expirationYear("29")
                .cvv("999")
                .cardholderName("Alice Smith")
                .build();
        wireMockServer.stubFor(post(urlEqualTo("/sessions"))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withBody("Gateway processing error")));

        // WHEN & THEN
        assertThrows(PaymentSessionFailedException.class, () -> paymentsAdapter.createSession(cardDetails));
        
        // Assert that exactly 1 request was made (no retries for 4xx errors)
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/sessions")));
    }

    @Test
    void paymentsAdapterShouldThrowExceptionWhenResponseIsEmpty() {
        // GIVEN
        CardDetails card = CardDetails.builder()
                .cardNumber("1234567890123456")
                .expirationMonth("11")
                .expirationYear("29")
                .cvv("999")
                .cardholderName("Alice Smith")
                .build();
        wireMockServer.stubFor(post(urlEqualTo("/sessions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")));

        // WHEN & THEN
        PaymentSessionFailedException ex = assertThrows(
                PaymentSessionFailedException.class,
                () -> paymentsAdapter.createSession(card)
        );
        assertTrue(ex.getMessage().contains("empty response"));
    }

    @Test
    void paymentsAdapterShouldThrowExceptionWhenResponseIsMalformed() {
        // GIVEN
        CardDetails card = CardDetails.builder()
                .cardNumber("1234567890123456")
                .expirationMonth("11")
                .expirationYear("29")
                .cvv("999")
                .cardholderName("Alice Smith")
                .build();
        wireMockServer.stubFor(post(urlEqualTo("/sessions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{malformed-json}")));

        // WHEN & THEN
        PaymentSessionFailedException ex = assertThrows(
                PaymentSessionFailedException.class,
                () -> paymentsAdapter.createSession(card)
        );
        assertTrue(ex.getMessage().contains("Unexpected failure during payment session creation"));
    }

    @Test
    void paymentsAdapterShouldRetryAndEventuallyThrowWhenConnectionFails() {
        // GIVEN
        CardDetails card = CardDetails.builder()
                .cardNumber("1234567890123456")
                .expirationMonth("11")
                .expirationYear("29")
                .cvv("999")
                .cardholderName("Alice Smith")
                .build();
        wireMockServer.stubFor(post(urlEqualTo("/sessions"))
                .willReturn(aResponse()
                        .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        // WHEN & THEN
        assertThrows(
                PaymentSessionFailedException.class,
                () -> paymentsAdapter.createSession(card)
        );

        // Should retry 3 times (4 requests total) and throw the original cause
        wireMockServer.verify(4, postRequestedFor(urlEqualTo("/sessions")));
    }
}
