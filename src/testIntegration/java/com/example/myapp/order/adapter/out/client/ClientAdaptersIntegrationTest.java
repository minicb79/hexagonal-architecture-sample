package com.example.myapp.order.adapter.out.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.example.myapp.order.domain.exception.CardNotFoundException;
import com.example.myapp.order.domain.exception.PaymentSessionFailedException;
import com.example.myapp.order.domain.model.CardDetails;
import com.example.myapp.order.domain.model.PaymentSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Instant;
import com.example.myapp.order.adapter.out.client.model.CardResponse;
import com.example.myapp.order.adapter.out.client.model.GatewaySessionRequest;
import com.example.myapp.order.adapter.out.client.model.GatewaySessionResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class ClientAdaptersIntegrationTest {

    private WireMockServer wireMockServer;
    private CardsServiceAdapter cardsAdapter;
    private PaymentsServiceAdapter paymentsAdapter;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0); // Binds to a dynamic free port
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .build();

        cardsAdapter = new CardsServiceAdapter(webClient);
        paymentsAdapter = new PaymentsServiceAdapter(webClient);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void cardsAdapterShouldRetrieveCardSuccessfully() {
        // GIVEN
        String cardId = "card-123";
        wireMockServer.stubFor(get(urlEqualTo("/cards/" + cardId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "card_number": "1234567890123456",
                                  "expiration_month": "11",
                                  "expiration_year": "29",
                                  "cvv": "999",
                                  "cardholder_name": "Alice Smith"
                                }
                                """)));

        // WHEN
        CardDetails cardDetails = cardsAdapter.getCardDetails(cardId);

        // THEN
        assertNotNull(cardDetails);
        assertEquals("1234567890123456", cardDetails.cardNumber());
        assertEquals("11", cardDetails.expirationMonth());
        assertEquals("29", cardDetails.expirationYear());
        assertEquals("999", cardDetails.cvv());
        assertEquals("Alice Smith", cardDetails.cardholderName());
    }

    @ParameterizedTest
    @CsvSource({
        "404, com.example.myapp.order.domain.exception.CardNotFoundException",
        "400, com.example.myapp.order.domain.exception.PaymentSessionFailedException",
        "500, com.example.myapp.order.domain.exception.PaymentSessionFailedException",
        "503, com.example.myapp.order.domain.exception.PaymentSessionFailedException"
    })
    void cardsAdapterShouldTranslateDownstreamErrorsCorrectly(int statusCode, String expectedExceptionClass) throws ClassNotFoundException {
        // GIVEN
        String cardId = "card-error";
        wireMockServer.stubFor(get(urlEqualTo("/cards/" + cardId))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withBody("Downstream error info")));

        @SuppressWarnings("unchecked")
        Class<? extends Throwable> exceptionType = (Class<? extends Throwable>) Class.forName(expectedExceptionClass);

        // WHEN & THEN
        assertThrows(exceptionType, () -> cardsAdapter.getCardDetails(cardId));
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
    void cardsAdapterShouldRecoverAfterTransientDownstreamFailures() {
        // GIVEN
        String cardId = "retry-card";
        wireMockServer.stubFor(get(urlEqualTo("/cards/" + cardId))
                .inScenario("Cards Retry Scenario")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willSetStateTo("First Failure")
                .willReturn(aResponse().withStatus(503)));

        wireMockServer.stubFor(get(urlEqualTo("/cards/" + cardId))
                .inScenario("Cards Retry Scenario")
                .whenScenarioStateIs("First Failure")
                .willSetStateTo("Second Failure")
                .willReturn(aResponse().withStatus(503)));

        wireMockServer.stubFor(get(urlEqualTo("/cards/" + cardId))
                .inScenario("Cards Retry Scenario")
                .whenScenarioStateIs("Second Failure")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "card_number": "1234567890123456",
                                  "expiration_month": "11",
                                  "expiration_year": "29",
                                  "cvv": "999",
                                  "cardholder_name": "Alice Smith"
                                }
                                """)));

        // WHEN
        CardDetails cardDetails = cardsAdapter.getCardDetails(cardId);

        // THEN
        assertNotNull(cardDetails);
        assertEquals("1234567890123456", cardDetails.cardNumber());
        
        // Verify exactly 3 calls were made (1 initial + 2 retries)
        wireMockServer.verify(3, getRequestedFor(urlEqualTo("/cards/" + cardId)));
    }

    @Test
    void cardsAdapterShouldThrowExceptionWhenMaxRetriesAreExhausted() {
        // GIVEN
        String cardId = "exhausted-card";
        wireMockServer.stubFor(get(urlEqualTo("/cards/" + cardId))
                .willReturn(aResponse().withStatus(503)));

        // WHEN & THEN
        assertThrows(PaymentSessionFailedException.class, () -> cardsAdapter.getCardDetails(cardId));

        // Verify exactly 4 calls were made (1 initial + 3 retries)
        wireMockServer.verify(4, getRequestedFor(urlEqualTo("/cards/" + cardId)));
    }
    @Test
    void cardsAdapterShouldThrowExceptionWhenResponseIsEmpty() {
        // GIVEN
        String cardId = "empty-card";
        wireMockServer.stubFor(get(urlEqualTo("/cards/" + cardId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")));

        // WHEN & THEN
        PaymentSessionFailedException ex = assertThrows(
                PaymentSessionFailedException.class,
                () -> cardsAdapter.getCardDetails(cardId)
        );
        assertTrue(ex.getMessage().contains("empty response"));
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
    void cardsAdapterShouldThrowExceptionWhenResponseIsMalformed() {
        // GIVEN
        String cardId = "malformed-card";
        wireMockServer.stubFor(get(urlEqualTo("/cards/" + cardId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{malformed-json}")));

        // WHEN & THEN
        PaymentSessionFailedException ex = assertThrows(
                PaymentSessionFailedException.class,
                () -> cardsAdapter.getCardDetails(cardId)
        );
        assertTrue(ex.getMessage().contains("Unexpected failure during card retrieval"));
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
    void cardsAdapterShouldRetryAndEventuallyThrowWhenConnectionFails() {
        // GIVEN
        String cardId = "fault-card";
        wireMockServer.stubFor(get(urlEqualTo("/cards/" + cardId))
                .willReturn(aResponse()
                        .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        // WHEN & THEN
        assertThrows(
                PaymentSessionFailedException.class,
                () -> cardsAdapter.getCardDetails(cardId)
        );
        
        // Should retry 3 times (4 requests total) and throw the original cause
        wireMockServer.verify(4, getRequestedFor(urlEqualTo("/cards/" + cardId)));
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
