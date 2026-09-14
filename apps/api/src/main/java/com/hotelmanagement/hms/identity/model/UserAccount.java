package com.hotelmanagement.hms.identity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "email",
            nullable = false,
            length = 255
    )
    private String email;

    @Column(
            name = "normalized_email",
            nullable = false,
            unique = true,
            length = 255
    )
    private String normalizedEmail;

    @Column(
            name = "password_hash",
            nullable = false,
            length = 255
    )
    private String passwordHash;

    @Column(
            name = "full_name",
            nullable = false,
            length = 200
    )
    private String fullName;

    @Column(
            name = "phone",
            length = 50
    )
    private String phone;

    @Column(
            name = "preferred_language",
            nullable = false,
            length = 10
    )
    private String preferredLanguage;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private UserStatus status;

    @Column(
            name = "failed_login_attempts",
            nullable = false
    )
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(
            name = "password_changed_at",
            nullable = false
    )
    private OffsetDateTime passwordChangedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    protected UserAccount() {
        // Required by JPA.
    }

    /**
     * Creates an active user account.
     *
     * passwordHash must already have been produced by the
     * application's PasswordEncoder.
     *
     * Plaintext passwords must never reach this entity.
     */
    public static UserAccount create(
            String email,
            String passwordHash,
            String fullName,
            String phone,
            String preferredLanguage,
            OffsetDateTime createdAt) {

        if (createdAt == null) {
            throw new IllegalArgumentException(
                    "Account creation time is required.");
        }

        UserAccount user =
                new UserAccount();

        user.setInitialEmail(email);

        user.passwordHash =
                requireText(
                        passwordHash,
                        "Password hash is required.");

        user.fullName =
                requireText(
                        fullName,
                        "Full name is required.");

        user.phone =
                trimToNull(phone);

        user.preferredLanguage =
                requireText(
                        preferredLanguage,
                        "Preferred language is required.")
                        .toLowerCase(Locale.ROOT);

        user.status =
                UserStatus.ACTIVE;

        user.failedLoginAttempts = 0;
        user.lockedUntil = null;
        user.lastLoginAt = null;

        user.passwordChangedAt =
                createdAt;

        user.createdAt =
                createdAt;

        user.updatedAt =
                createdAt;

        return user;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNormalizedEmail() {
        return normalizedEmail;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public UserStatus getStatus() {
        return status;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public OffsetDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setEmail(
            String email,
            OffsetDateTime updatedAt) {

        this.email =
                normalizeEmail(email);

        this.normalizedEmail =
                normalizeEmail(email);

        touch(updatedAt);
    }

    public void setFullName(
            String fullName,
            OffsetDateTime updatedAt) {

        this.fullName =
                requireText(
                        fullName,
                        "Full name is required.");

        touch(updatedAt);
    }

    public void setPhone(
            String phone,
            OffsetDateTime updatedAt) {

        this.phone =
                trimToNull(phone);

        touch(updatedAt);
    }

    public void setPreferredLanguage(
            String preferredLanguage,
            OffsetDateTime updatedAt) {

        this.preferredLanguage =
                requireText(
                        preferredLanguage,
                        "Preferred language is required.")
                        .toLowerCase(Locale.ROOT);

        touch(updatedAt);
    }

    /**
     * Changes the encoded password.
     *
     * A password reset also clears temporary failed-login locking.
     * It does not reactivate an administratively DISABLED account.
     */
    public void changePassword(
            String passwordHash,
            OffsetDateTime changedAt) {

        this.passwordHash =
                requireText(
                        passwordHash,
                        "Password hash is required.");

        this.passwordChangedAt =
                requireTimestamp(
                        changedAt,
                        "Password change time is required.");

        this.failedLoginAttempts = 0;
        this.lockedUntil = null;

        if (status == UserStatus.LOCKED) {
            status = UserStatus.ACTIVE;
        }

        touch(changedAt);
    }

    public void recordSuccessfulLogin(
            OffsetDateTime loginAt) {

        this.lastLoginAt =
                requireTimestamp(
                        loginAt,
                        "Login time is required.");

        this.failedLoginAttempts = 0;
        this.lockedUntil = null;

        touch(loginAt);
    }

    public void recordFailedLogin(
            OffsetDateTime attemptedAt) {

        requireTimestamp(
                attemptedAt,
                "Login attempt time is required.");

        this.failedLoginAttempts++;

        touch(attemptedAt);
    }

    public void lockUntil(
            OffsetDateTime lockedUntil,
            OffsetDateTime updatedAt) {

        OffsetDateTime normalizedUpdatedAt =
                requireTimestamp(
                        updatedAt,
                        "Update time is required.");

        OffsetDateTime normalizedLockedUntil =
                requireTimestamp(
                        lockedUntil,
                        "Lock expiration time is required.");

        if (!normalizedLockedUntil
                .isAfter(normalizedUpdatedAt)) {

            throw new IllegalArgumentException(
                    "Lock expiration must be "
                            + "after the current time.");
        }

        this.status =
                UserStatus.LOCKED;

        this.lockedUntil =
                normalizedLockedUntil;

        touch(normalizedUpdatedAt);
    }

    /**
     * Administrative/security disable.
     *
     * Disabled accounts remain disabled until explicitly activated.
     */
    public void disable(
            OffsetDateTime updatedAt) {

        this.status =
                UserStatus.DISABLED;

        this.lockedUntil = null;

        touch(updatedAt);
    }

    /**
     * Activates or reactivates the account.
     *
     * Also resets login lock counters.
     */
    public void activate(
            OffsetDateTime updatedAt) {

        this.status =
                UserStatus.ACTIVE;

        this.failedLoginAttempts = 0;
        this.lockedUntil = null;

        touch(updatedAt);
    }

    private void setInitialEmail(
            String email) {

        String normalized =
                normalizeEmail(email);

        this.email =
                normalized;

        this.normalizedEmail =
                normalized;
    }

    private void touch(
            OffsetDateTime updatedAt) {

        this.updatedAt =
                requireTimestamp(
                        updatedAt,
                        "Update time is required.");
    }

    private static String normalizeEmail(
            String email) {

        return requireText(
                email,
                "Email is required.")
                .toLowerCase(Locale.ROOT);
    }

    private static String requireText(
            String value,
            String errorMessage) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    errorMessage);
        }

        return value.trim();
    }

    private static OffsetDateTime requireTimestamp(
            OffsetDateTime value,
            String errorMessage) {

        if (value == null) {
            throw new IllegalArgumentException(
                    errorMessage);
        }

        return value;
    }

    private static String trimToNull(
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