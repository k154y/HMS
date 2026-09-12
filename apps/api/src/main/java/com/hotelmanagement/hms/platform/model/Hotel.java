package com.hotelmanagement.hms.platform.model;

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
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "tin", length = 100)
    private String tin;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "timezone", nullable = false, length = 100)
    private String timezone;

    @Column(name = "default_language", nullable = false, length = 10)
    private String defaultLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private HotelStatus status;

    @Column(name = "trial_started_at", nullable = false)
    private OffsetDateTime trialStartedAt;

    @Column(name = "trial_ends_at", nullable = false)
    private OffsetDateTime trialEndsAt;

    @Column(name = "grace_period_ends_at")
    private OffsetDateTime gracePeriodEndsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Hotel() {
        // Required by JPA.
    }

    /**
     * Creates a new hotel tenant that starts on the HMS free trial.
     *
     * The service layer calculates the trial dates so subscription
     * policy remains explicit and testable outside the persistence model.
     */
    public static Hotel createTrial(
            String code,
            String legalName,
            String displayName,
            String tin,
            String phone,
            String email,
            String address,
            String currencyCode,
            String timezone,
            String defaultLanguage,
            OffsetDateTime trialStartedAt,
            OffsetDateTime trialEndsAt) {

        Hotel hotel = new Hotel();

        hotel.code = code;
        hotel.legalName = legalName;
        hotel.displayName = displayName;
        hotel.tin = tin;
        hotel.phone = phone;
        hotel.email = email;
        hotel.address = address;
        hotel.currencyCode = currencyCode;
        hotel.timezone = timezone;
        hotel.defaultLanguage = defaultLanguage;

        hotel.status = HotelStatus.TRIAL;

        hotel.trialStartedAt = trialStartedAt;
        hotel.trialEndsAt = trialEndsAt;

        hotel.createdAt = trialStartedAt;
        hotel.updatedAt = trialStartedAt;

        return hotel;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTin() {
        return tin;
    }

    public void setTin(String tin) {
        this.tin = tin;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public HotelStatus getStatus() {
        return status;
    }

    public void setStatus(HotelStatus status) {
        this.status = status;
    }

    public OffsetDateTime getTrialStartedAt() {
        return trialStartedAt;
    }

    public void setTrialStartedAt(OffsetDateTime trialStartedAt) {
        this.trialStartedAt = trialStartedAt;
    }

    public OffsetDateTime getTrialEndsAt() {
        return trialEndsAt;
    }

    public void setTrialEndsAt(OffsetDateTime trialEndsAt) {
        this.trialEndsAt = trialEndsAt;
    }

    public OffsetDateTime getGracePeriodEndsAt() {
        return gracePeriodEndsAt;
    }

    public void setGracePeriodEndsAt(OffsetDateTime gracePeriodEndsAt) {
        this.gracePeriodEndsAt = gracePeriodEndsAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}