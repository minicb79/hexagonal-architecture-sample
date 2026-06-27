package com.example.myapp.order.application.port.in;

import com.example.myapp.order.domain.model.CardDetails;
import com.example.myapp.order.domain.model.CardType;

/**
 * Command DTO for creating a payment session.
 * Uses the Self-Validating Command pattern in the constructor.
 */
public record CreatePaymentSessionCommand(
    CardType cardType,
    String cardId,
    CardDetails inlineCardDetails
) {
    public CreatePaymentSessionCommand {
        if (cardType == null) {
            throw new IllegalArgumentException("Card type must not be null.");
        }
        
        // Self-validation business constraints
        if (cardType == CardType.SAVED_CARD || cardType == CardType.AUTOPAY_CARD) {
            if (cardId == null || cardId.isBlank()) {
                throw new IllegalArgumentException("Card ID is required when using a saved or autopay card.");
            }
        }
        
        if (cardType == CardType.NEW_CARD) {
            if (inlineCardDetails == null) {
                throw new IllegalArgumentException("Inline card details are required when using a new card.");
            }
        }
    }
}
