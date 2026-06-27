package com.example.myapp.order.adapter.out.client;

import com.example.myapp.order.application.port.out.PaymentClientPort;
import com.example.myapp.order.domain.exception.PaymentSessionFailedException;
import com.example.myapp.order.domain.model.CardDetails;
import com.example.myapp.order.domain.model.PaymentSession;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Driven adapter implementing PaymentClientPort using WebClient.
 */
public class PaymentsServiceAdapter implements PaymentClientPort {

    private final WebClient webClient;

    public PaymentsServiceAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public PaymentSession createSession(CardDetails cardDetails) {
        GatewaySessionRequest request = new GatewaySessionRequest(
                cardDetails.cardNumber(),
                cardDetails.expirationMonth(),
                cardDetails.expirationYear(),
                cardDetails.cvv(),
                cardDetails.cardholderName()
        );

        try {
            GatewaySessionResponse response = webClient.post()
                    .uri("/sessions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GatewaySessionResponse.class)
                    .block();

            if (response == null) {
                throw new PaymentSessionFailedException("Received empty response from payments service.");
            }

            return new PaymentSession(
                    response.sessionId(),
                    response.status(),
                    Instant.parse(response.expiresAt())
            );
        } catch (WebClientResponseException e) {
            throw new PaymentSessionFailedException(
                    "Failed to create payment session from downstream gateway: [Status " + e.getStatusCode() + "] " + e.getResponseBodyAsString(), e
            );
        } catch (Exception e) {
            throw new PaymentSessionFailedException("Unexpected failure during payment session creation: " + e.getMessage(), e);
        }
    }

    /**
     * Package-private DTO representing the downstream API payload.
     */
    record GatewaySessionRequest(
            @JsonProperty("card_number") String cardNumber,
            @JsonProperty("expiration_month") String expirationMonth,
            @JsonProperty("expiration_year") String expirationYear,
            @JsonProperty("cvv") String cvv,
            @JsonProperty("cardholder_name") String cardholderName
    ) {}

    /**
     * Package-private DTO representing the downstream API response.
     */
    record GatewaySessionResponse(
            @JsonProperty("session_id") String sessionId,
            @JsonProperty("status") String status,
            @JsonProperty("expires_at") String expiresAt
    ) {}
}
