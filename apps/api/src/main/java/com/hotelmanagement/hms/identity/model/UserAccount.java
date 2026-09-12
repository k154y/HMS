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
            length = 30
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
     * Creates an active HMS identity.
     *
     * The supplied password must already be securely hashed by the
     * identity service before reaching this persistence model.
     */
    public static UserAccount create(
            String email,
            String normalizedEmail,
            String passwordHash,
            String fullName,
            String phone,
            String preferredLanguage,
            OffsetDateTime createdAt) {

        UserAccount user = new UserAccount();

        user.email = email;
        user.normalizedEmail = normalizedEmail;
        user.passwordHash = passwordHash;
        user.fullName = fullName;
        user.phone = phone;
        user.preferredLanguage = preferredLanguage;

        user.status = UserStatus.ACTIVE;

        user.failedLoginAttempts = 0;
        user.lockedUntil = null;
        user.lastLoginAt = null;

        user.passwordChangedAt = createdAt;
        user.createdAt = createdAt;
        user.updatedAt = createdAt;

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
            String normalizedEmail,
            OffsetDateTime updatedAt) {

        this.email = email;
        this.normalizedEmail = normalizedEmail;
        this.updatedAt = updatedAt;
    }

    public void setFullName(
            String fullName,
            OffsetDateTime updatedAt) {

        this.fullName = fullName;
        this.updatedAt = updatedAt;
    }

    public void setPhone(
            String phone,
            OffsetDateTime updatedAt) {

        this.phone = phone;
        this.updatedAt = updatedAt;
    }

    public void setPreferredLanguage(
            String preferredLanguage,
            OffsetDateTime updatedAt) {

        this.preferredLanguage = preferredLanguage;
        this.updatedAt = updatedAt;
    }

    /**
     * Replaces the password hash after a successful password change
     * or administrative reset.
     */
    public void changePassword(
            String passwordHash,
            OffsetDateTime changedAt) {

        this.passwordHash = passwordHash;
        this.passwordChangedAt = changedAt;
        this.updatedAt = changedAt;

        /*
         * A successful password reset also clears temporary
         * failed-login lock state.
         */
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;

        if (this.status == UserStatus.LOCKED) {
            this.status = UserStatus.ACTIVE;
        }
    }

    public void recordSuccessfulLogin(
            OffsetDateTime loginAt) {

        this.lastLoginAt = loginAt;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;

        if (this.status == UserStatus.LOCKED) {
            this.status = UserStatus.ACTIVE;
        }

        this.updatedAt = loginAt;
    }

    public void recordFailedLogin(
            OffsetDateTime updatedAt) {

        this.failedLoginAttempts++;
        this.updatedAt = updatedAt;
    }

    public void lockUntil(
            OffsetDateTime lockedUntil,
            OffsetDateTime updatedAt) {

        this.status = UserStatus.LOCKED;
        this.lockedUntil = lockedUntil;
        this.updatedAt = updatedAt;
    }

    public void disable(
            OffsetDateTime updatedAt) {

        this.status = UserStatus.DISABLED;
        this.updatedAt = updatedAt;
    }

    public void activate(
            OffsetDateTime updatedAt) {

        this.status = UserStatus.ACTIVE;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = updatedAt;
    }
}