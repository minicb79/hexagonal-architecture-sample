package com.example.myapp.order.application.port.out.cardsservice;

import com.example.myapp.order.domain.model.CardDetails;

/**
 * Outbound port (SPI) to fetch saved card attributes.
 */
public interface CardClientPort {
    CardDetails getCardDetails(String cardId);
}
