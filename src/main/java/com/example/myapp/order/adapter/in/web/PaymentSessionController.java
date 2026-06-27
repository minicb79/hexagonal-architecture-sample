package com.example.myapp.order.adapter.in.web;

import com.example.myapp.order.adapter.in.web.api.DefaultApi;
import com.example.myapp.order.adapter.in.web.dto.CreateSessionRequest;
import com.example.myapp.order.adapter.in.web.dto.InlineCardDetails;
import com.example.myapp.order.adapter.in.web.dto.PaymentSessionResponse;
import com.example.myapp.order.application.port.in.CreatePaymentSessionCommand;
import com.example.myapp.order.application.port.in.CreatePaymentSessionUseCase;
import com.example.myapp.order.domain.model.CardDetails;
import com.example.myapp.order.domain.model.CardType;
import com.example.myapp.order.domain.model.PaymentSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.time.ZoneOffset;

/**
 * Driving adapter (REST Controller) implementing the generated OpenAPI DefaultApi.
 */
@RestController
public class PaymentSessionController implements DefaultApi {

    private final CreatePaymentSessionUseCase createPaymentSessionUseCase;

    public PaymentSessionController(CreatePaymentSessionUseCase createPaymentSessionUseCase) {
        this.createPaymentSessionUseCase = createPaymentSessionUseCase;
    }

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
            inlineDetails = new CardDetails(
                    inlineDto.getCardNumber(),
                    inlineDto.getExpirationMonth(),
                    inlineDto.getExpirationYear(),
                    inlineDto.getCvv(),
                    inlineDto.getCardholderName()
            );
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
