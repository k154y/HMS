package com.hotelmanagement.hms.identity.authorization.decision;

import java.util.Objects;

public record AuthorizationDecision(
        boolean allowed,
        AuthorizationDenialReason denialReason
) {

    public AuthorizationDecision {

        if (allowed && denialReason != null) {
            throw new IllegalArgumentException(
                    "An allowed authorization decision "
                            + "cannot contain a denial reason.");
        }

        if (!allowed && denialReason == null) {
            throw new IllegalArgumentException(
                    "A denied authorization decision "
                            + "must contain a denial reason.");
        }
    }

    public static AuthorizationDecision allow() {
        return new AuthorizationDecision(
                true,
                null
        );
    }

    public static AuthorizationDecision deny(
            AuthorizationDenialReason reason) {

        return new AuthorizationDecision(
                false,
                Objects.requireNonNull(reason)
        );
    }
}