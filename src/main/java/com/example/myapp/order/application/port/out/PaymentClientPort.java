package com.example.myapp.order.application.port.out;

import com.example.myapp.order.domain.model.CardDetails;
import com.example.myapp.order.domain.model.PaymentSession;

/**
 * Outbound port (SPI) to initiate payment session creation with a gateway.
 */
public interface PaymentClientPort {
    PaymentSession createSession(CardDetails cardDetails);
}
