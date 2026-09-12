package com.hotelmanagement.hms.platform.model;

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
@Table(name = "branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "timezone", length = 100)
    private String timezone;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Branch() {
        // Required by JPA.
    }

    /**
     * Creates a new operational branch belonging to a hotel tenant.
     *
     * Branch ownership is mandatory so later reservations, rooms,
     * payments, inventory, and operational reports can be safely
     * scoped to both hotel and branch where applicable.
     */
    public static Branch create(
            Hotel hotel,
            String code,
            String name,
            String phone,
            String email,
            String address,
            String timezone,
            OffsetDateTime createdAt) {

        Branch branch = new Branch();

        branch.hotel = hotel;
        branch.code = code;
        branch.name = name;
        branch.phone = phone;
        branch.email = email;
        branch.address = address;
        branch.timezone = timezone;
        branch.active = true;
        branch.createdAt = createdAt;
        branch.updatedAt = createdAt;

        return branch;
    }

    public UUID getId() {
        return id;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getTimezone() {
        return timezone;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}