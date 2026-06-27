package com.example.myapp.order.application.port.in;

import com.example.myapp.order.domain.model.PaymentSession;

/**
 * Inbound port (Use Case) defining payment session creation.
 */
public interface CreatePaymentSessionUseCase {
    PaymentSession createSession(CreatePaymentSessionCommand command);
}
