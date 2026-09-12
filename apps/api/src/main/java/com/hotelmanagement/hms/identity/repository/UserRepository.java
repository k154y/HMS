package com.hotelmanagement.hms.identity.repository;

import com.hotelmanagement.hms.identity.model.UserAccount;
import com.hotelmanagement.hms.identity.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository
        extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByNormalizedEmail(
            String normalizedEmail
    );

    Optional<UserAccount> findByIdAndStatus(
            UUID id,
            UserStatus status
    );

    boolean existsByNormalizedEmail(
            String normalizedEmail
    );
}