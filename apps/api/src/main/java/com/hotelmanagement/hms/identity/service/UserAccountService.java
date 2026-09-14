package com.hotelmanagement.hms.identity.service;

import com.hotelmanagement.hms.identity.authentication.password.PasswordPolicy;
import com.hotelmanagement.hms.identity.model.UserAccount;
import com.hotelmanagement.hms.identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
public class UserAccountService {

    private static final String DEFAULT_LANGUAGE =
            "en";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public UserAccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy) {

        this.userRepository =
                userRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.passwordPolicy =
                passwordPolicy;
    }

    /**
     * Creates a new global HMS identity.
     *
     * This is an internal application operation.
     * It is not currently exposed as public user registration.
     */
    @Transactional
    public UserAccount createAccount(
            String email,
            String rawPassword,
            String fullName,
            String phone,
            String preferredLanguage) {

        String normalizedEmail =
                normalizeEmail(email);

        if (userRepository
                .existsByNormalizedEmail(
                        normalizedEmail)) {

            throw new IllegalStateException(
                    "A user account with this email "
                            + "already exists.");
        }

        validateFullName(
                fullName);

        String normalizedLanguage =
                normalizeLanguage(
                        preferredLanguage);

        passwordPolicy
                .validateNewPassword(
                        rawPassword);

        String passwordHash =
                passwordEncoder.encode(
                        rawPassword);

        OffsetDateTime now =
                OffsetDateTime.now(
                        ZoneOffset.UTC);

        UserAccount user =
                UserAccount.create(
                        normalizedEmail,
                        passwordHash,
                        fullName.trim(),
                        trimToNull(phone),
                        normalizedLanguage,
                        now
                );

        return userRepository
                .saveAndFlush(
                        user);
    }

    private String normalizeEmail(
            String email) {

        if (email == null
                || email.isBlank()) {

            throw new IllegalArgumentException(
                    "Email is required.");
        }

        String normalized =
                email
                        .trim()
                        .toLowerCase(
                                Locale.ROOT);

        if (normalized.length() > 255) {
            throw new IllegalArgumentException(
                    "Email must not exceed "
                            + "255 characters.");
        }

        return normalized;
    }

    private void validateFullName(
            String fullName) {

        if (fullName == null
                || fullName.isBlank()) {

            throw new IllegalArgumentException(
                    "Full name is required.");
        }

        if (fullName.trim().length() > 200) {
            throw new IllegalArgumentException(
                    "Full name must not exceed "
                            + "200 characters.");
        }
    }

    private String normalizeLanguage(
            String preferredLanguage) {

        String language =
                preferredLanguage == null
                        || preferredLanguage.isBlank()
                        ? DEFAULT_LANGUAGE
                        : preferredLanguage
                                .trim()
                                .toLowerCase(
                                        Locale.ROOT);

        return switch (language) {
            case "en", "fr", "rw" ->
                    language;

            default ->
                    throw new IllegalArgumentException(
                            "Supported languages are "
                                    + "en, fr, and rw.");
        };
    }

    private String trimToNull(
            String value) {

        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }
}