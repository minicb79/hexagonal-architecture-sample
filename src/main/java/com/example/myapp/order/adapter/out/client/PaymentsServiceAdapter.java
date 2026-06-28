package com.example.myapp.order.adapter.out.client;

import com.example.myapp.order.application.port.out.PaymentClientPort;
import com.example.myapp.order.domain.exception.PaymentSessionFailedException;
import com.example.myapp.order.domain.model.CardDetails;
import com.example.myapp.order.domain.model.PaymentSession;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.example.myapp.order.adapter.out.client.model.GatewaySessionRequest;
import com.example.myapp.order.adapter.out.client.model.GatewaySessionResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

/**
 * Driven adapter implementing PaymentClientPort using WebClient.
 */
@RequiredArgsConstructor
public class PaymentsServiceAdapter implements PaymentClientPort {

    private final WebClient webClient;

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
                    .retryWhen(reactor.util.retry.Retry.backoff(3, java.time.Duration.ofMillis(50))
                            .filter(this::isTransientException)
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                    .block();

            if (response == null) {
                throw new PaymentSessionFailedException("Received empty response from payments service.");
            }

            return PaymentSession.builder()
                    .sessionId(response.sessionId())
                    .status(response.status())
                    .expiresAt(Instant.parse(response.expiresAt()))
                    .build();
        } catch (WebClientResponseException e) {
            throw new PaymentSessionFailedException(
                    "Failed to create payment session from downstream gateway: [Status " + e.getStatusCode() + "] " + e.getResponseBodyAsString(), e
            );
        } catch (Exception e) {
            throw new PaymentSessionFailedException("Unexpected failure during payment session creation: " + e.getMessage(), e);
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
