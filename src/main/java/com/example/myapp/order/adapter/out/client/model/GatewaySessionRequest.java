package com.example.myapp.order.adapter.out.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Downstream API payload representation for payment sessions creation.
 * Public for subpackage access, guarded by ArchUnit boundaries.
 */
public record GatewaySessionRequest(
        @JsonProperty("card_number") String cardNumber,
        @JsonProperty("expiration_month") String expirationMonth,
        @JsonProperty("expiration_year") String expirationYear,
        @JsonProperty("cvv") String cvv,
        @JsonProperty("cardholder_name") String cardholderName
) {}
