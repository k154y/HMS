package com.hotelmanagement.hms.identity.membership.dto;

import com.hotelmanagement.hms.identity.membership.model.HotelMembership;
import com.hotelmanagement.hms.identity.membership.model.HotelMembershipStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HotelMembershipResponse(
        UUID id,
        UUID userId,
        UUID hotelId,
        HotelMembershipStatus status,
        boolean allBranches,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String fullName,
        String email
) {

    public static HotelMembershipResponse from(
            HotelMembership membership) {

        return new HotelMembershipResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getHotel().getId(),
                membership.getStatus(),
                membership.hasAllBranches(),
                membership.getCreatedAt(),
                membership.getUpdatedAt(),membership.getUser().getFullName(),membership.getUser().getEmail()
        );
    }
}
