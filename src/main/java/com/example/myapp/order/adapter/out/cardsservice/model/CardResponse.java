package com.example.myapp.order.adapter.out.cardsservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Downstream API representation for cards retrieval.
 * Public for subpackage access, guarded by ArchUnit boundaries.
 */
public record CardResponse(
        @JsonProperty("card_number") String cardNumber,
        @JsonProperty("expiration_month") String expirationMonth,
        @JsonProperty("expiration_year") String expirationYear,
        @JsonProperty("cvv") String cvv,
        @JsonProperty("cardholder_name") String cardholderName
) {}
