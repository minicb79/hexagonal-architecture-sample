package com.example.myapp.order.adapter.out.cardsservice;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.example.myapp.order.domain.exception.CardNotFoundException;
import com.example.myapp.order.domain.exception.PaymentSessionFailedException;
import com.example.myapp.order.domain.model.CardDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class CardsServiceAdapterIntegrationTest {

    private WireMockServer wireMockServer;
    private CardsServiceAdapter cardsAdapter;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .build();

        cardsAdapter = new CardsServiceAdapter(webClient);
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
}
