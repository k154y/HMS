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

@Service
public class AuthenticationService {

    private static final String GENERIC_BAD_CREDENTIALS =
            "Invalid email or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshSessionService refreshSessionService;
    private final AuthenticationProperties authenticationProperties;

    /*
     * Used when an email does not exist so password verification still
     * performs expensive password-hashing work.
     *
     * This helps reduce obvious timing differences between:
     *
     *     existing email
     *     non-existing email
     *
     * and therefore makes account enumeration more difficult.
     */
    private final String dummyPasswordHash;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshSessionService refreshSessionService,
            AuthenticationProperties authenticationProperties) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshSessionService = refreshSessionService;
        this.authenticationProperties =
                authenticationProperties;

        this.dummyPasswordHash =
                passwordEncoder.encode(
                        "HMS_TIMING_PROTECTION_ONLY");
    }

    /**
     * Authenticates a user and creates an access-token +
     * refresh-session pair.
     */
    @Transactional
    public AuthenticationResponse login(
            LoginRequest request) {

        String normalizedEmail =
                normalizeEmail(
                        request.email());

        UserAccount user =
                userRepository
                        .findByNormalizedEmail(
                                normalizedEmail)
                        .orElse(null);

        /*
         * Always perform a password verification operation.
         *
         * For an unknown user we verify against a dummy hash instead
         * of immediately returning. This reduces obvious timing-based
         * email enumeration.
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

            handleFailedLogin(
                    user,
                    now);

            throw new BadCredentialsException(
                    GENERIC_BAD_CREDENTIALS);
        }

        user.recordSuccessfulLogin(
                now);

        UserAccount savedUser =
                userRepository.saveAndFlush(
                        user);

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
     * Rotates an existing refresh session and issues a new
     * short-lived access token.
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
     * Revokes the supplied refresh session.
     */
    @Transactional
    public void logout(
            RefreshTokenRequest request) {

        refreshSessionService
                .revokeForLogout(
                        request.refreshToken());
    }

    /**
     * Evaluates global account state before credentials are accepted.
     */
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
         * LOCKED with no end timestamp is treated as locked until
         * an administrative/security action changes the account.
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
         * Temporary lock period has expired.
         *
         * Reset counters before processing the new login attempt.
         */
        user.activate(now);

        userRepository.saveAndFlush(
                user);
    }

    /**
     * Records one invalid password attempt and applies a temporary
     * account lock when the configured threshold is reached.
     */
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