package com.example.myapp.order.adapter.out.client;

import com.example.myapp.order.application.port.out.CardClientPort;
import com.example.myapp.order.domain.exception.CardNotFoundException;
import com.example.myapp.order.domain.exception.PaymentSessionFailedException;
import com.example.myapp.order.domain.model.CardDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Driven adapter implementing CardClientPort using WebClient.
 */
public class CardsServiceAdapter implements CardClientPort {

    private final WebClient webClient;

    public CardsServiceAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public CardDetails getCardDetails(String cardId) {
        try {
            CardResponse response = webClient.get()
                    .uri("/cards/{id}", cardId)
                    .retrieve()
                    .bodyToMono(CardResponse.class)
                    .block();

            if (response == null) {
                throw new PaymentSessionFailedException("Received empty response from cards service.");
            }

            return new CardDetails(
                    response.cardNumber(),
                    response.expirationMonth(),
                    response.expirationYear(),
                    response.cvv(),
                    response.cardholderName()
            );
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CardNotFoundException("Saved card with ID " + cardId + " not found.", e);
            }
            throw new PaymentSessionFailedException("Failed to retrieve card from downstream service: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new PaymentSessionFailedException("Unexpected failure during card retrieval: " + e.getMessage(), e);
        }
    }

    /**
     * Package-private DTO representing the downstream API response to prevent core pollution.
     */
    record CardResponse(
            @JsonProperty("card_number") String cardNumber,
            @JsonProperty("expiration_month") String expirationMonth,
            @JsonProperty("expiration_year") String expirationYear,
            @JsonProperty("cvv") String cvv,
            @JsonProperty("cardholder_name") String cardholderName
    ) {}
}
