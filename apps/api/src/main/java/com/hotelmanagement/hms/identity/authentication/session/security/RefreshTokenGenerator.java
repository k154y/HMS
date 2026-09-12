package com.hotelmanagement.hms.identity.authentication.session.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class RefreshTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom =
            new SecureRandom();

    /**
     * Generates a new high-entropy refresh token.
     *
     * The raw token must only be returned to the client.
     * Only its hash should be persisted.
     */
    public RefreshTokenMaterial generate() {

        byte[] randomBytes =
                new byte[TOKEN_BYTES];

        secureRandom.nextBytes(
                randomBytes);

        String rawToken =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                randomBytes);

        String tokenHash =
                hash(rawToken);

        return new RefreshTokenMaterial(
                rawToken,
                tokenHash
        );
    }

    /**
     * Hashes a refresh token supplied by the client so it can
     * be compared against the value stored in PostgreSQL.
     */
    public String hash(
            String rawToken) {

        if (rawToken == null
                || rawToken.isBlank()) {

            throw new IllegalArgumentException(
                    "Refresh token is required.");
        }

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256");

            byte[] hashBytes =
                    digest.digest(
                            rawToken.getBytes(
                                    StandardCharsets.UTF_8));

            return HexFormat
                    .of()
                    .formatHex(
                            hashBytes);

        } catch (NoSuchAlgorithmException exception) {

            /*
             * SHA-256 is required by the Java platform.
             * If it is unavailable, the JVM/security provider
             * environment is unusable for this authentication flow.
             */
            throw new IllegalStateException(
                    "SHA-256 is unavailable.",
                    exception
            );
        }
    }

    /**
     * Raw token + persisted hash generated together.
     *
     * Never log rawToken.
     */
    public record RefreshTokenMaterial(
            String rawToken,
            String tokenHash
    ) {

        public RefreshTokenMaterial {

            if (rawToken == null
                    || rawToken.isBlank()) {

                throw new IllegalArgumentException(
                        "Raw refresh token is required.");
            }

            if (tokenHash == null
                    || tokenHash.isBlank()) {

                throw new IllegalArgumentException(
                        "Refresh token hash is required.");
            }
        }
    }
}