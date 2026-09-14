package com.hotelmanagement.hms.identity.membership.repository;

import com.hotelmanagement.hms.identity.membership.model.HotelMembership;
import com.hotelmanagement.hms.identity.membership.model.HotelMembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelMembershipRepository
        extends JpaRepository<HotelMembership, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from HotelMembership m where m.id=:id and m.hotel.id=:hotel")
    Optional<HotelMembership> findLockedInHotel(UUID id, UUID hotel);

    org.springframework.data.domain.Page<HotelMembership> findByHotel_Id(UUID hotelId, org.springframework.data.domain.Pageable pageable);

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
