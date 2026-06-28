package com.example.myapp.order.adapter.in.rest;

import com.example.myapp.order.adapter.in.rest.api.DefaultApi;
import com.example.myapp.order.adapter.in.rest.dto.CreateSessionRequest;
import com.example.myapp.order.adapter.in.rest.dto.InlineCardDetails;
import com.example.myapp.order.adapter.in.rest.dto.PaymentSessionResponse;
import com.example.myapp.order.application.port.in.CreatePaymentSessionCommand;
import com.example.myapp.order.application.port.in.CreatePaymentSessionUseCase;
import com.example.myapp.order.domain.model.CardDetails;
import com.example.myapp.order.domain.model.CardType;
import com.example.myapp.order.domain.model.PaymentSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.time.ZoneOffset;

import lombok.RequiredArgsConstructor;

/**
 * Driving adapter (REST Controller) implementing the generated OpenAPI DefaultApi.
 */
@RestController
@RequiredArgsConstructor
public class PaymentSessionController implements DefaultApi {

    private final CreatePaymentSessionUseCase createPaymentSessionUseCase;

    @Override
    public ResponseEntity<PaymentSessionResponse> createPaymentSession(CreateSessionRequest request) {
        // 1. Translate DTO parameters to Core Application Command
        CreatePaymentSessionCommand command = toCoreCommand(request);

        // 2. Invoke core Use Case
        PaymentSession session = createPaymentSessionUseCase.createSession(command);

        // 3. Map result to response DTO
        PaymentSessionResponse response = toResponseDto(session);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private CreatePaymentSessionCommand toCoreCommand(CreateSessionRequest request) {
        CardType cardType = CardType.valueOf(request.getCardType().getValue());
        
        CardDetails inlineDetails = null;
        if (request.getInlineCardDetails() != null) {
            InlineCardDetails inlineDto = request.getInlineCardDetails();
            inlineDetails = CardDetails.builder()
                    .cardNumber(inlineDto.getCardNumber())
                    .expirationMonth(inlineDto.getExpirationMonth())
                    .expirationYear(inlineDto.getExpirationYear())
                    .cvv(inlineDto.getCvv())
                    .cardholderName(inlineDto.getCardholderName())
                    .build();
        }

        return new CreatePaymentSessionCommand(cardType, request.getCardId(), inlineDetails);
    }

    private PaymentSessionResponse toResponseDto(PaymentSession session) {
        PaymentSessionResponse response = new PaymentSessionResponse();
        response.setSessionId(session.sessionId());
        response.setStatus(session.status());
        // Map java.time.Instant to OffsetDateTime as required by generated DTO
        response.setExpiresAt(session.expiresAt().atOffset(ZoneOffset.UTC));
        return response;
    }
}
