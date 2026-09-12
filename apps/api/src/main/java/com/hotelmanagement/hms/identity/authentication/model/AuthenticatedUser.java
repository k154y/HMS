package com.hotelmanagement.hms.identity.authentication.model;

import java.util.UUID;

/**
 * Minimal authenticated identity used by application services.
 *
 * Authorization information is intentionally not stored here.
 * Hotel membership, branch access, roles, and permissions are
 * evaluated by the tenant authorization layer.
 */
public record AuthenticatedUser(
        UUID userId,
        String email
) {

    public AuthenticatedUser {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "Authenticated user ID is required.");
        }
    }
}