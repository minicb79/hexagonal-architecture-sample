package com.example.myapp.order.adapter.out.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Downstream API response representation for payment sessions creation.
 * Public for subpackage access, guarded by ArchUnit boundaries.
 */
public record GatewaySessionResponse(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("status") String status,
        @JsonProperty("expires_at") String expiresAt
) {}
