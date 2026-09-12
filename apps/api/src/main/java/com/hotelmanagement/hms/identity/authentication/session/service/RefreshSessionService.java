package com.hotelmanagement.hms.identity.authentication.session.service;

import com.hotelmanagement.hms.identity.authentication.jwt.JwtProperties;
import com.hotelmanagement.hms.identity.authentication.jwt.JwtTokenService;
import com.hotelmanagement.hms.identity.authentication.model.AccessToken;
import com.hotelmanagement.hms.identity.authentication.session.model.RefreshSession;
import com.hotelmanagement.hms.identity.authentication.session.repository.RefreshSessionRepository;
import com.hotelmanagement.hms.identity.authentication.session.security.RefreshTokenGenerator;
import com.hotelmanagement.hms.identity.authentication.session.security.RefreshTokenGenerator.RefreshTokenMaterial;
import com.hotelmanagement.hms.identity.model.UserAccount;
import com.hotelmanagement.hms.identity.model.UserStatus;
import com.hotelmanagement.hms.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshSessionService {

    private static final String REASON_LOGOUT = "LOGOUT";
    private static final String REASON_ROTATED = "TOKEN_ROTATED";
    private static final String REASON_SECURITY = "SECURITY_REVOCATION";

    private final RefreshSessionRepository sessionRepository;
    private final RefreshTokenGenerator tokenGenerator;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public RefreshSessionService(
            RefreshSessionRepository sessionRepository,
            RefreshTokenGenerator tokenGenerator,
            UserRepository userRepository,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties) {

        this.sessionRepository = sessionRepository;
        this.tokenGenerator = tokenGenerator;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Creates a new refresh session after successful authentication.
     *
     * The returned raw token must be sent to the client once.
     * Only its hash is persisted.
     */
    @Transactional
    public CreatedRefreshSession createSession(
            UserAccount user) {

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "A persisted user is required.");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Refresh sessions cannot be created "
                            + "for inactive accounts.");
        }

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        OffsetDateTime expiresAt =
                now.plusDays(
                        jwtProperties.refreshTokenDays());

        RefreshTokenMaterial material =
                tokenGenerator.generate();

        RefreshSession session =
                RefreshSession.create(
                        user,
                        material.tokenHash(),
                        expiresAt,
                        now
                );

        RefreshSession savedSession =
                sessionRepository.saveAndFlush(
                        session);

        return new CreatedRefreshSession(
                savedSession.getId(),
                material.rawToken(),
                expiresAt
        );
    }

    /**
     * Rotates a valid refresh token.
     *
     * Old refresh token:
     *     becomes revoked.
     *
     * New refresh token:
     *     becomes the only usable successor token.
     *
     * A new JWT access token is also issued.
     */
    @Transactional
    public RotatedSession rotate(
            String rawRefreshToken) {

        String tokenHash =
                tokenGenerator.hash(
                        rawRefreshToken);

        RefreshSession currentSession =
                sessionRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid refresh token."));

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        if (currentSession.isRevoked()) {
            throw new IllegalStateException(
                    "Refresh token has been revoked.");
        }

        if (currentSession.isExpired(now)) {
            throw new IllegalStateException(
                    "Refresh token has expired.");
        }

        UserAccount user =
                userRepository
                        .findById(
                                currentSession
                                        .getUser()
                                        .getId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "User account no longer exists."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "User account is not active.");
        }

        currentSession.recordUse(now);

        CreatedRefreshSession replacement =
                createSession(user);

        currentSession.revoke(
                now,
                REASON_ROTATED,
                replacement.sessionId()
        );

        sessionRepository.saveAndFlush(
                currentSession);

        AccessToken accessToken =
                jwtTokenService.issueAccessToken(
                        user);

        return new RotatedSession(
                accessToken,
                replacement.rawRefreshToken(),
                replacement.expiresAt()
        );
    }

    /**
     * Revokes one refresh token during logout.
     *
     * Unknown tokens are rejected instead of silently appearing
     * successful inside the domain service.
     */
    @Transactional
    public void revokeForLogout(
            String rawRefreshToken) {

        String tokenHash =
                tokenGenerator.hash(
                        rawRefreshToken);

        RefreshSession session =
                sessionRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid refresh token."));

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        session.revoke(
                now,
                REASON_LOGOUT,
                null
        );

        sessionRepository.saveAndFlush(
                session);
    }

    /**
     * Revokes every currently active refresh session for a user.
     *
     * Useful after:
     *
     * - password change;
     * - administrator security action;
     * - suspected account compromise.
     */
    @Transactional
    public void revokeAllForUser(
            UUID userId) {

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        List<RefreshSession> activeSessions =
                sessionRepository
                        .findByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(
                                userId,
                                now
                        );

        for (RefreshSession session
                : activeSessions) {

            session.revoke(
                    now,
                    REASON_SECURITY,
                    null
            );
        }

        sessionRepository.saveAll(
                activeSessions);
    }

    public record CreatedRefreshSession(
            UUID sessionId,
            String rawRefreshToken,
            OffsetDateTime expiresAt
    ) {
    }

    public record RotatedSession(
            AccessToken accessToken,
            String refreshToken,
            OffsetDateTime refreshTokenExpiresAt
    ) {
    }
}