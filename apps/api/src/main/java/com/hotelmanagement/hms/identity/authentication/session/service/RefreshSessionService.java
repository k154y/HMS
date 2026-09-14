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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshSessionService {

    private static final String REASON_LOGOUT =
            "LOGOUT";

    private static final String REASON_ROTATED =
            "TOKEN_ROTATED";

    private static final String REASON_SECURITY =
            "SECURITY_REVOCATION";

    private static final String INVALID_REFRESH_TOKEN =
            "Invalid refresh token.";

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
     * Only the hash is persisted.
     * The raw refresh credential is returned once to the caller.
     */
    @Transactional
    public CreatedRefreshSession createSession(
            UserAccount user) {

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "A persisted user is required.");
        }

        user = userRepository.findLockedById(user.getId())
                .orElseThrow(RefreshSessionService::invalidRefreshToken);

        if (user.getStatus()
                != UserStatus.ACTIVE) {

            throw new DisabledException(
                    "User account is not active.");
        }

        OffsetDateTime now =
                OffsetDateTime.now(
                        ZoneOffset.UTC);

        OffsetDateTime expiresAt =
                now.plusDays(
                        jwtProperties
                                .refreshTokenDays());

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
                sessionRepository
                        .saveAndFlush(
                                session);

        return new CreatedRefreshSession(
                savedSession.getId(),
                material.rawToken(),
                expiresAt
        );
    }

    /**
     * Rotates one valid refresh session.
     *
     * The old token becomes unusable immediately after successful
     * transaction commit.
     */
    @Transactional
    public RotatedSession rotate(
            String rawRefreshToken) {

        String tokenHash =
                hashRefreshToken(
                        rawRefreshToken);

        lockSessionOwner(tokenHash);

        RefreshSession currentSession =
                sessionRepository
                        .findByTokenHash(
                                tokenHash)
                        .orElseThrow(
                                RefreshSessionService
                                        ::invalidRefreshToken
                        );

        OffsetDateTime now =
                OffsetDateTime.now(
                        ZoneOffset.UTC);

        if (currentSession.isRevoked()) {
            throw invalidRefreshToken();
        }

        if (currentSession.isExpired(now)) {
            throw invalidRefreshToken();
        }

        UserAccount user =
                userRepository
                        .findById(
                                currentSession
                                        .getUser()
                                        .getId())
                        .orElseThrow(
                                RefreshSessionService
                                        ::invalidRefreshToken
                        );

        if (user.getStatus()
                != UserStatus.ACTIVE) {

            throw new DisabledException(
                    "User account is not active.");
        }

        currentSession.recordUse(
                now);

        CreatedRefreshSession replacement =
                createSession(
                        user);

        currentSession.revoke(
                now,
                REASON_ROTATED,
                replacement.sessionId()
        );

        sessionRepository
                .saveAndFlush(
                        currentSession);

        AccessToken accessToken =
                jwtTokenService
                        .issueAccessToken(
                                user);

        return new RotatedSession(
                accessToken,
                replacement.rawRefreshToken(),
                replacement.expiresAt()
        );
    }

    /**
     * Revokes one refresh session during logout.
     *
     * The session must belong to the account represented by the
     * already validated access JWT.
     */
    @Transactional
    public void revokeForLogout(
            UUID authenticatedUserId,
            String rawRefreshToken) {

        if (authenticatedUserId == null) {
            throw invalidRefreshToken();
        }

        String tokenHash =
                hashRefreshToken(
                        rawRefreshToken);

        lockSessionOwner(tokenHash);

        RefreshSession session =
                sessionRepository
                        .findByTokenHash(
                                tokenHash)
                        .orElseThrow(
                                RefreshSessionService
                                        ::invalidRefreshToken
                        );

        UUID sessionUserId =
                session
                        .getUser()
                        .getId();

        if (!authenticatedUserId.equals(
                sessionUserId)) {

            /*
             * Do not reveal that the supplied refresh credential
             * belongs to another account.
             */
            throw invalidRefreshToken();
        }

        if (session.isRevoked()) {
            throw invalidRefreshToken();
        }

        OffsetDateTime now =
                OffsetDateTime.now(
                        ZoneOffset.UTC);

        session.revoke(
                now,
                REASON_LOGOUT,
                null
        );

        sessionRepository
                .saveAndFlush(
                        session);
    }

    /**
     * Revokes all currently usable refresh sessions belonging
     * to one user.
     *
     * Intended for security-sensitive events such as password
     * changes or administrator account intervention.
     */
    @Transactional
    public void revokeAllForUser(
            UUID userId) {

        userRepository.findLockedById(userId)
                .orElseThrow(RefreshSessionService::invalidRefreshToken);

        OffsetDateTime now =
                OffsetDateTime.now(
                        ZoneOffset.UTC);

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

        sessionRepository
                .saveAll(
                        activeSessions);
    }

    // All session mutations lock the user first, so password changes, login,
    // rotation and logout serialize without session/user lock-order inversion.
    private void lockSessionOwner(String tokenHash) {
        UUID userId = sessionRepository.findUserIdByTokenHash(tokenHash)
                .orElseThrow(RefreshSessionService::invalidRefreshToken);
        userRepository.findLockedById(userId)
                .orElseThrow(RefreshSessionService::invalidRefreshToken);
    }

    private String hashRefreshToken(
            String rawRefreshToken) {

        try {
            return tokenGenerator.hash(
                    rawRefreshToken);
        } catch (IllegalArgumentException exception) {
            throw invalidRefreshToken();
        }
    }

    private static BadCredentialsException
    invalidRefreshToken() {

        return new BadCredentialsException(
                INVALID_REFRESH_TOKEN);
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
