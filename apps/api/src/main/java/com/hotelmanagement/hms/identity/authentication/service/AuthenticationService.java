package com.hotelmanagement.hms.identity.authentication.service;

import com.hotelmanagement.hms.identity.authentication.config.AuthenticationProperties;
import com.hotelmanagement.hms.identity.authentication.dto.AuthenticationResponse;
import com.hotelmanagement.hms.identity.authentication.dto.LoginRequest;
import com.hotelmanagement.hms.identity.authentication.dto.RefreshTokenRequest;
import com.hotelmanagement.hms.identity.authentication.jwt.JwtTokenService;
import com.hotelmanagement.hms.identity.authentication.model.AccessToken;
import com.hotelmanagement.hms.identity.authentication.session.service.RefreshSessionService;
import com.hotelmanagement.hms.identity.authentication.session.service.RefreshSessionService.CreatedRefreshSession;
import com.hotelmanagement.hms.identity.authentication.session.service.RefreshSessionService.RotatedSession;
import com.hotelmanagement.hms.identity.model.UserAccount;
import com.hotelmanagement.hms.identity.model.UserStatus;
import com.hotelmanagement.hms.identity.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthenticationService {

    private static final String GENERIC_BAD_CREDENTIALS =
            "Invalid email or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshSessionService refreshSessionService;
    private final AuthenticationProperties authenticationProperties;
    private final com.hotelmanagement.hms.audit.service.AuditService audit;
    private final io.micrometer.core.instrument.Counter failures;

    /*
     * Used for timing protection when an email is not found.
     */
    private final String dummyPasswordHash;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshSessionService refreshSessionService,
            AuthenticationProperties authenticationProperties,
            com.hotelmanagement.hms.audit.service.AuditService audit,
            io.micrometer.core.instrument.MeterRegistry metrics) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshSessionService = refreshSessionService;
        this.authenticationProperties =
                authenticationProperties;
        this.audit = audit;
        this.failures = metrics.counter("hms.authentication.failures");

        this.dummyPasswordHash =
                passwordEncoder.encode(
                        "HMS_TIMING_PROTECTION_ONLY");
    }

    /**
     * Authenticates a user and creates an access-token +
     * refresh-session pair.
     */
    // Failed-attempt state must commit even when credentials are rejected.
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthenticationResponse login(
            LoginRequest request) {

        String normalizedEmail =
                normalizeEmail(
                        request.email());

        UserAccount user =
                userRepository
                        .findLockedByEmail(
                                normalizedEmail)
                        .orElse(null);

        /*
         * Always execute password verification.
         *
         * For a nonexistent account, verify against a dummy hash
         * to reduce obvious account-enumeration timing differences.
         */
        String encodedPassword =
                user == null
                        ? dummyPasswordHash
                        : user.getPasswordHash();

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        encodedPassword);

        if (user == null) {
            failures.increment();
            throw new BadCredentialsException(
                    GENERIC_BAD_CREDENTIALS);
        }

        OffsetDateTime now =
                OffsetDateTime.now(
                        ZoneOffset.UTC);

        handleAccountState(
                user,
                now);

        if (!passwordMatches) {
            failures.increment();

            handleFailedLogin(
                    user,
                    now);
            audit.record(null, null, user.getId(), "LOGIN_FAILED", "USER", user.getId());

            throw new BadCredentialsException(
                    GENERIC_BAD_CREDENTIALS);
        }

        user.recordSuccessfulLogin(
                now);

        UserAccount savedUser =
                userRepository.saveAndFlush(
                        user);
        audit.record(null, null, user.getId(), "LOGIN_SUCCEEDED", "USER", user.getId());

        AccessToken accessToken =
                jwtTokenService
                        .issueAccessToken(
                                savedUser);

        CreatedRefreshSession refreshSession =
                refreshSessionService
                        .createSession(
                                savedUser);

        return AuthenticationResponse.from(
                accessToken,
                refreshSession
        );
    }

    /**
     * Rotates a refresh token and returns a fresh token pair.
     */
    @Transactional
    public AuthenticationResponse refresh(
            RefreshTokenRequest request) {

        RotatedSession rotatedSession =
                refreshSessionService.rotate(
                        request.refreshToken());

        return AuthenticationResponse.from(
                rotatedSession);
    }

    /**
     * Revokes one refresh session owned by the currently
     * authenticated account.
     */
    @Transactional
    public void logout(
            UUID authenticatedUserId,
            RefreshTokenRequest request) {

        if (authenticatedUserId == null) {
            throw new BadCredentialsException(
                    "Authentication is required.");
        }

        refreshSessionService
                .revokeForLogout(
                        authenticatedUserId,
                        request.refreshToken()
                );
        audit.record(null, null, authenticatedUserId, "LOGOUT", "USER", authenticatedUserId);
    }

    private void handleAccountState(
            UserAccount user,
            OffsetDateTime now) {

        if (user.getStatus()
                == UserStatus.DISABLED) {

            throw new DisabledException(
                    "Account is disabled.");
        }

        if (user.getStatus()
                != UserStatus.LOCKED) {

            return;
        }

        OffsetDateTime lockedUntil =
                user.getLockedUntil();

        /*
         * LOCKED without an expiry is considered an indefinite
         * security/administrative lock.
         */
        if (lockedUntil == null) {
            throw new LockedException(
                    "Account is locked.");
        }

        if (lockedUntil.isAfter(now)) {
            throw new LockedException(
                    "Account is temporarily locked.");
        }

        /*
         * Temporary lock has expired.
         */
        user.activate(now);

        userRepository.saveAndFlush(
                user);
    }

    private void handleFailedLogin(
            UserAccount user,
            OffsetDateTime now) {

        user.recordFailedLogin(
                now);

        if (user.getFailedLoginAttempts()
                >= authenticationProperties
                        .maxFailedAttempts()) {

            OffsetDateTime lockedUntil =
                    now.plusMinutes(
                            authenticationProperties
                                    .lockMinutes());

            user.lockUntil(
                    lockedUntil,
                    now
            );
        }

        userRepository.saveAndFlush(
                user);
    }

    private String normalizeEmail(
            String email) {

        if (email == null
                || email.isBlank()) {

            throw new BadCredentialsException(
                    GENERIC_BAD_CREDENTIALS);
        }

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
