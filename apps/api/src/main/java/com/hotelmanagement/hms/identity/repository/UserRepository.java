package com.hotelmanagement.hms.identity.repository;

import com.hotelmanagement.hms.identity.model.UserAccount;
import com.hotelmanagement.hms.identity.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository
        extends JpaRepository<UserAccount, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UserAccount u where u.id = :id")
    Optional<UserAccount> findLockedById(UUID id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UserAccount u where u.normalizedEmail = :email")
    Optional<UserAccount> findLockedByEmail(String email);

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
