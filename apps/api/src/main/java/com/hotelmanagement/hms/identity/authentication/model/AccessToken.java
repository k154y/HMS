package com.hotelmanagement.hms.identity.authentication.model;

import java.time.Instant;

public record AccessToken(
        String token,
        String tokenType,
        Instant issuedAt,
        Instant expiresAt
) {

    public AccessToken {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "Access token is required.");
        }

        if (issuedAt == null) {
            throw new IllegalArgumentException(
                    "Issued-at timestamp is required.");
        }

        if (expiresAt == null) {
            throw new IllegalArgumentException(
                    "Expiration timestamp is required.");
        }

        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "Access-token expiration must be "
                            + "after its issue time.");
        }
    }

    public static AccessToken bearer(
            String token,
            Instant issuedAt,
            Instant expiresAt) {

        return new AccessToken(
                token,
                "Bearer",
                issuedAt,
                expiresAt
        );
    }

    public long expiresInSeconds() {
        return expiresAt
                .getEpochSecond()
                - issuedAt.getEpochSecond();
    }
}