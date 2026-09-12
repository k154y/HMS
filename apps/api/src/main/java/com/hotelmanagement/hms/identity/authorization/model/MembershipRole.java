package com.hotelmanagement.hms.identity.authorization.model;

import com.hotelmanagement.hms.identity.membership.model.HotelMembership;
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
@Table(name = "membership_roles")
public class MembershipRole {

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
     * Explicit tenant identifier used by composite database
     * constraints to prevent cross-hotel role assignments.
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
            name = "role_id",
            nullable = false
    )
    private Role role;

    @Column(
            name = "assigned_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime assignedAt;

    protected MembershipRole() {
        // Required by JPA.
    }

    public static MembershipRole create(
            HotelMembership membership,
            Role role,
            OffsetDateTime assignedAt) {

        UUID membershipHotelId =
                membership
                        .getHotel()
                        .getId();

        UUID roleHotelId =
                role
                        .getHotel()
                        .getId();

        if (!membershipHotelId.equals(roleHotelId)) {
            throw new IllegalArgumentException(
                    "The membership and role "
                            + "must belong to the same hotel.");
        }

        MembershipRole membershipRole =
                new MembershipRole();

        membershipRole.membership =
                membership;

        membershipRole.hotelId =
                membershipHotelId;

        membershipRole.role =
                role;

        membershipRole.assignedAt =
                assignedAt;

        return membershipRole;
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

    public Role getRole() {
        return role;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }
}