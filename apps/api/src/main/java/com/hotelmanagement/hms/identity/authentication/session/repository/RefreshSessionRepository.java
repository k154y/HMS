package com.hotelmanagement.hms.identity.authentication.session.repository;

import com.hotelmanagement.hms.identity.authentication.session.model.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository
        extends JpaRepository<RefreshSession, UUID> {

    Optional<RefreshSession> findByTokenHash(
            String tokenHash
    );

    List<RefreshSession>
    findByUser_IdAndRevokedAtIsNullOrderByCreatedAtDesc(
            UUID userId
    );

    List<RefreshSession>
    findByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(
            UUID userId,
            OffsetDateTime now
    );

    long countByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(
            UUID userId,
            OffsetDateTime now
    );
}