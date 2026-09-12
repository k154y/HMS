package com.hotelmanagement.hms.identity.authorization.model;

import com.hotelmanagement.hms.platform.model.Hotel;
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
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "hotel_id",
            nullable = false
    )
    private Hotel hotel;

    @Column(
            name = "code",
            nullable = false,
            length = 100
    )
    private String code;

    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;

    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;

    @Column(
            name = "system_defined",
            nullable = false
    )
    private boolean systemDefined;

    @Column(
            name = "active",
            nullable = false
    )
    private boolean active;

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

    protected Role() {
        // Required by JPA.
    }

    public static Role create(
            Hotel hotel,
            String code,
            String name,
            String description,
            boolean systemDefined,
            OffsetDateTime createdAt) {

        Role role = new Role();

        role.hotel = hotel;
        role.code = code;
        role.name = name;
        role.description = description;

        role.systemDefined = systemDefined;
        role.active = true;

        role.createdAt = createdAt;
        role.updatedAt = createdAt;

        return role;
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

    public String getDescription() {
        return description;
    }

    public boolean isSystemDefined() {
        return systemDefined;
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

    public void rename(
            String name,
            String description,
            OffsetDateTime updatedAt) {

        this.name = name;
        this.description = description;
        this.updatedAt = updatedAt;
    }

    public void deactivate(
            OffsetDateTime updatedAt) {

        this.active = false;
        this.updatedAt = updatedAt;
    }

    public void activate(
            OffsetDateTime updatedAt) {

        this.active = true;
        this.updatedAt = updatedAt;
    }
}