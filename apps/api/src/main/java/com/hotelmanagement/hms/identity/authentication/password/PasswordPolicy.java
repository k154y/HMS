package com.hotelmanagement.hms.identity.authentication.password;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public static final int MINIMUM_LENGTH = 15;
    public static final int MAXIMUM_LENGTH = 128;

    /**
     * Validates a new password before hashing.
     *
     * The password is intentionally NOT trimmed because leading
     * or trailing spaces may legitimately be part of a password.
     */
    public void validateNewPassword(
            String rawPassword) {

        if (rawPassword == null) {
            throw new IllegalArgumentException(
                    "Password is required.");
        }

        if (rawPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "Password cannot contain only whitespace.");
        }

        int length =
                rawPassword.codePointCount(
                        0,
                        rawPassword.length()
                );

        if (length < MINIMUM_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must contain at least "
                            + MINIMUM_LENGTH
                            + " characters.");
        }

        if (length > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must contain no more than "
                            + MAXIMUM_LENGTH
                            + " characters.");
        }
    }
}