package com.hotelmanagement.hms.identity.membership.model;

import com.hotelmanagement.hms.identity.model.UserAccount;
import com.hotelmanagement.hms.platform.model.Hotel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "hotel_memberships")
public class HotelMembership {

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

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "hotel_id",
            nullable = false
    )
    private Hotel hotel;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private HotelMembershipStatus status;

    @Column(
            name = "all_branches",
            nullable = false
    )
    private boolean allBranches;

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

    protected HotelMembership() {
        // Required by JPA.
    }

    /**
     * Creates an active hotel membership.
     *
     * allBranches = true:
     *     user may work across every branch of the hotel.
     *
     * allBranches = false:
     *     branch access must be granted explicitly through
     *     membership_branch_access.
     */
    public static HotelMembership create(
            UserAccount user,
            Hotel hotel,
            boolean allBranches,
            OffsetDateTime createdAt) {

        HotelMembership membership =
                new HotelMembership();

        membership.user = user;
        membership.hotel = hotel;
        membership.status =
                HotelMembershipStatus.ACTIVE;

        membership.allBranches = allBranches;

        membership.createdAt = createdAt;
        membership.updatedAt = createdAt;

        return membership;
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public HotelMembershipStatus getStatus() {
        return status;
    }

    public boolean hasAllBranches() {
        return allBranches;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void grantAllBranches(
            OffsetDateTime updatedAt) {

        this.allBranches = true;
        this.updatedAt = updatedAt;
    }

    public void restrictToSelectedBranches(
            OffsetDateTime updatedAt) {

        this.allBranches = false;
        this.updatedAt = updatedAt;
    }

    public void suspend(
            OffsetDateTime updatedAt) {

        this.status =
                HotelMembershipStatus.SUSPENDED;

        this.updatedAt = updatedAt;
    }

    public void revoke(
            OffsetDateTime updatedAt) {

        this.status =
                HotelMembershipStatus.REVOKED;

        this.updatedAt = updatedAt;
    }

    public void activate(
            OffsetDateTime updatedAt) {

        this.status =
                HotelMembershipStatus.ACTIVE;

        this.updatedAt = updatedAt;
    }
}