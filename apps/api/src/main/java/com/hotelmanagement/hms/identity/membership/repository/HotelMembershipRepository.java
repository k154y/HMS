package com.hotelmanagement.hms.identity.membership.repository;

import com.hotelmanagement.hms.identity.membership.model.HotelMembership;
import com.hotelmanagement.hms.identity.membership.model.HotelMembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelMembershipRepository
        extends JpaRepository<HotelMembership, UUID> {

    Optional<HotelMembership> findByUser_IdAndHotel_Id(
            UUID userId,
            UUID hotelId
    );

    Optional<HotelMembership> findByIdAndHotel_Id(
            UUID membershipId,
            UUID hotelId
    );

    boolean existsByUser_IdAndHotel_Id(
            UUID userId,
            UUID hotelId
    );

    List<HotelMembership> findByHotel_IdOrderByCreatedAtAsc(
            UUID hotelId
    );

    List<HotelMembership> findByUser_IdAndStatus(
            UUID userId,
            HotelMembershipStatus status
    );
}