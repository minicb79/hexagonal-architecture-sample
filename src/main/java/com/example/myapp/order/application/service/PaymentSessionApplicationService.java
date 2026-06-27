package com.example.myapp.order.application.service;

import com.example.myapp.order.application.port.in.CreatePaymentSessionCommand;
import com.example.myapp.order.application.port.in.CreatePaymentSessionUseCase;
import com.example.myapp.order.application.port.out.CardClientPort;
import com.example.myapp.order.application.port.out.PaymentClientPort;
import com.example.myapp.order.domain.model.CardDetails;
import com.example.myapp.order.domain.model.PaymentSession;

/**
 * Orchestrates the payment session creation use case.
 * Free of Spring stereotype annotations (instantiated via Config class).
 */
public class PaymentSessionApplicationService implements CreatePaymentSessionUseCase {

    private final CardClientPort cardClientPort;
    private final PaymentClientPort paymentClientPort;

    public PaymentSessionApplicationService(
            CardClientPort cardClientPort,
            PaymentClientPort paymentClientPort) {
        this.cardClientPort = cardClientPort;
        this.paymentClientPort = paymentClientPort;
    }

    @Override
    public PaymentSession createSession(CreatePaymentSessionCommand command) {
        // 1. Resolve card details based on type
        CardDetails cardDetails = switch (command.cardType()) {
            case SAVED_CARD, AUTOPAY_CARD -> cardClientPort.getCardDetails(command.cardId());
            case NEW_CARD -> command.inlineCardDetails();
        };

        // 2. Delegate to the Payments provider using the resolved card details
        return paymentClientPort.createSession(cardDetails);
    }
}
