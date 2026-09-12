package com.hotelmanagement.hms.identity.membership.model;

import com.hotelmanagement.hms.platform.model.Branch;
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
@Table(name = "membership_branch_access")
public class MembershipBranchAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "membership_id",
            nullable = false
    )
    private HotelMembership membership;

    /**
     * Stored explicitly because the database uses hotel_id
     * as part of composite foreign keys to prevent granting
     * access across hotel tenant boundaries.
     */
    @Column(
            name = "hotel_id",
            nullable = false
    )
    private UUID hotelId;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "branch_id",
            nullable = false
    )
    private Branch branch;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    protected MembershipBranchAccess() {
        // Required by JPA.
    }

    public static MembershipBranchAccess create(
            HotelMembership membership,
            Branch branch,
            OffsetDateTime createdAt) {

        if (!membership
                .getHotel()
                .getId()
                .equals(
                        branch
                                .getHotel()
                                .getId())) {

            throw new IllegalArgumentException(
                    "The branch and membership "
                            + "must belong to the same hotel.");
        }

        MembershipBranchAccess access =
                new MembershipBranchAccess();

        access.membership = membership;

        access.hotelId =
                membership
                        .getHotel()
                        .getId();

        access.branch = branch;

        access.createdAt = createdAt;

        return access;
    }

    public UUID getId() {
        return id;
    }

    public HotelMembership getMembership() {
        return membership;
    }

    public UUID getHotelId() {
        return hotelId;
    }

    public Branch getBranch() {
        return branch;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}