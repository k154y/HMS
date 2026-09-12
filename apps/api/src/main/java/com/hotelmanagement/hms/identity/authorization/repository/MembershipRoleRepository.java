package com.hotelmanagement.hms.identity.authorization.repository;

import com.hotelmanagement.hms.identity.authorization.model.MembershipRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRoleRepository
        extends JpaRepository<MembershipRole, UUID> {

    boolean existsByMembership_IdAndRole_Id(
            UUID membershipId,
            UUID roleId
    );

    Optional<MembershipRole>
    findByMembership_IdAndRole_Id(
            UUID membershipId,
            UUID roleId
    );

    List<MembershipRole>
    findByMembership_IdOrderByRole_NameAsc(
            UUID membershipId
    );

    List<MembershipRole>
    findByHotelIdAndRole_Id(
            UUID hotelId,
            UUID roleId
    );

    void deleteByMembership_Id(
            UUID membershipId
    );
}