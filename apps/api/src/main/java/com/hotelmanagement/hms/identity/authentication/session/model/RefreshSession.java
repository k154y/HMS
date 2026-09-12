package com.hotelmanagement.hms.identity.authentication.session.model;

import com.hotelmanagement.hms.identity.model.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private UserAccount user;

    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String tokenHash;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private OffsetDateTime expiresAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(
            name = "revocation_reason",
            length = 100
    )
    private String revocationReason;

    @Column(name = "replaced_by_session_id")
    private UUID replacedBySessionId;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    protected RefreshSession() {
        // Required by JPA.
    }

    public static RefreshSession create(
            UserAccount user,
            String tokenHash,
            OffsetDateTime expiresAt,
            OffsetDateTime createdAt) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "Refresh session user is required.");
        }

        if (tokenHash == null
                || tokenHash.isBlank()) {

            throw new IllegalArgumentException(
                    "Refresh token hash is required.");
        }

        if (expiresAt == null
                || createdAt == null
                || !expiresAt.isAfter(createdAt)) {

            throw new IllegalArgumentException(
                    "Refresh-session expiration must be "
                            + "after creation time.");
        }

        RefreshSession session =
                new RefreshSession();

        session.user = user;
        session.tokenHash = tokenHash;
        session.expiresAt = expiresAt;

        session.lastUsedAt = null;

        session.revokedAt = null;
        session.revocationReason = null;

        session.replacedBySessionId = null;

        session.createdAt = createdAt;

        return session;
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public UUID getReplacedBySessionId() {
        return replacedBySessionId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(
            OffsetDateTime now) {

        return !expiresAt.isAfter(now);
    }

    public boolean isUsable(
            OffsetDateTime now) {

        return !isRevoked()
                && !isExpired(now);
    }

    public void recordUse(
            OffsetDateTime usedAt) {

        if (isRevoked()) {
            throw new IllegalStateException(
                    "A revoked refresh session "
                            + "cannot be used.");
        }

        this.lastUsedAt = usedAt;
    }

    /**
     * Revokes the session.
     *
     * replacedBySessionId is supplied when token rotation created
     * a successor session. It remains null for events such as
     * logout or administrative/security revocation.
     */
    public void revoke(
            OffsetDateTime revokedAt,
            String reason,
            UUID replacedBySessionId) {

        if (this.revokedAt != null) {
            return;
        }

        if (revokedAt == null) {
            throw new IllegalArgumentException(
                    "Revocation timestamp is required.");
        }

        if (reason == null
                || reason.isBlank()) {

            throw new IllegalArgumentException(
                    "Revocation reason is required.");
        }

        this.revokedAt = revokedAt;
        this.revocationReason =
                reason.trim();

        this.replacedBySessionId =
                replacedBySessionId;
    }
}