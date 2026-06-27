package com.example.myapp.order.application.service;

import com.example.myapp.order.application.port.in.CreatePaymentSessionCommand;
import com.example.myapp.order.application.port.out.CardClientPort;
import com.example.myapp.order.application.port.out.PaymentClientPort;
import com.example.myapp.order.domain.model.CardDetails;
import com.example.myapp.order.domain.model.CardType;
import com.example.myapp.order.domain.model.PaymentSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentSessionApplicationServiceTest {

    @Mock
    private CardClientPort cardClientPort;

    @Mock
    private PaymentClientPort paymentClientPort;

    @InjectMocks
    private PaymentSessionApplicationService service;

    private final CardDetails mockCard = new CardDetails(
        "1234567890123456", "12", "28", "123", "John Doe"
    );

    private final PaymentSession mockSession = new PaymentSession(
        "session-abc-123", "APPROVED", Instant.now().plusSeconds(3600)
    );

    @Test
    void createSessionWithSavedCardShouldQueryCardsPortAndReturnSession() {
        // GIVEN
        CreatePaymentSessionCommand command = new CreatePaymentSessionCommand(
            CardType.SAVED_CARD, "card-id-777", null
        );
        when(cardClientPort.getCardDetails("card-id-777")).thenReturn(mockCard);
        when(paymentClientPort.createSession(mockCard)).thenReturn(mockSession);

        // WHEN
        PaymentSession result = service.createSession(command);

        // THEN
        assertNotNull(result);
        assertEquals(mockSession, result);
        verify(cardClientPort, times(1)).getCardDetails("card-id-777");
        verify(paymentClientPort, times(1)).createSession(mockCard);
    }

    @Test
    void createSessionWithAutopayCardShouldQueryCardsPortAndReturnSession() {
        // GIVEN
        CreatePaymentSessionCommand command = new CreatePaymentSessionCommand(
            CardType.AUTOPAY_CARD, "card-id-888", null
        );
        when(cardClientPort.getCardDetails("card-id-888")).thenReturn(mockCard);
        when(paymentClientPort.createSession(mockCard)).thenReturn(mockSession);

        // WHEN
        PaymentSession result = service.createSession(command);

        // THEN
        assertNotNull(result);
        assertEquals(mockSession, result);
        verify(cardClientPort, times(1)).getCardDetails("card-id-888");
        verify(paymentClientPort, times(1)).createSession(mockCard);
    }

    @Test
    void createSessionWithNewCardShouldUseInlineDetailsAndNotQueryCardsPort() {
        // GIVEN
        CreatePaymentSessionCommand command = new CreatePaymentSessionCommand(
            CardType.NEW_CARD, null, mockCard
        );
        when(paymentClientPort.createSession(mockCard)).thenReturn(mockSession);

        // WHEN
        PaymentSession result = service.createSession(command);

        // THEN
        assertNotNull(result);
        assertEquals(mockSession, result);
        verifyNoInteractions(cardClientPort); // Asserts that the Cards API Client is NOT called
        verify(paymentClientPort, times(1)).createSession(mockCard);
    }

    @Test
    void commandInstantiationShouldValidateSemanticsAndThrowExceptions() {
        // Missing Card ID for SAVED_CARD
        assertThrows(IllegalArgumentException.class, () ->
            new CreatePaymentSessionCommand(CardType.SAVED_CARD, null, null)
        );

        // Missing Card Details for NEW_CARD
        assertThrows(IllegalArgumentException.class, () ->
            new CreatePaymentSessionCommand(CardType.NEW_CARD, "card-id-999", null)
        );

        // Missing Card Type
        assertThrows(IllegalArgumentException.class, () ->
            new CreatePaymentSessionCommand(null, "card-id-999", null)
        );
    }
}
