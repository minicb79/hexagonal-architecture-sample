package com.example.myapp.order.adapter.out.cardsservice;

import com.example.myapp.order.application.port.out.cardsservice.CardClientPort;
import com.example.myapp.order.domain.exception.CardNotFoundException;
import com.example.myapp.order.domain.exception.PaymentSessionFailedException;
import com.example.myapp.order.domain.model.CardDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.example.myapp.order.adapter.out.cardsservice.model.CardResponse;
import lombok.RequiredArgsConstructor;

/**
 * Driven adapter implementing CardClientPort using WebClient.
 */
@RequiredArgsConstructor
public class CardsServiceAdapter implements CardClientPort {

    private final WebClient webClient;

    @Override
    public CardDetails getCardDetails(String cardId) {
        try {
            CardResponse response = webClient.get()
                    .uri("/cards/{id}", cardId)
                    .retrieve()
                    .bodyToMono(CardResponse.class)
                    .retryWhen(reactor.util.retry.Retry.backoff(3, java.time.Duration.ofMillis(50))
                            .filter(this::isTransientException)
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                    .block();

            if (response == null) {
                throw new PaymentSessionFailedException("Received empty response from cards service.");
            }

            return CardDetails.builder()
                    .cardNumber(response.cardNumber())
                    .expirationMonth(response.expirationMonth())
                    .expirationYear(response.expirationYear())
                    .cvv(response.cvv())
                    .cardholderName(response.cardholderName())
                    .build();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CardNotFoundException("Saved card with ID " + cardId + " not found.", e);
            }
            throw new PaymentSessionFailedException("Failed to retrieve card from downstream service: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new PaymentSessionFailedException("Unexpected failure during card retrieval: " + e.getMessage(), e);
        }
    }

    private boolean isTransientException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            return wcre.getStatusCode().is5xxServerError();
        }
        Throwable cause = throwable;
        if (throwable instanceof org.springframework.web.reactive.function.client.WebClientRequestException) {
            cause = throwable.getCause();
        }
        return cause instanceof java.io.IOException 
                || cause instanceof java.util.concurrent.TimeoutException;
    }

}
