package com.hotelmanagement.hms.identity.membership.repository;

import com.hotelmanagement.hms.identity.membership.model.MembershipBranchAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipBranchAccessRepository
        extends JpaRepository<MembershipBranchAccess, UUID> {

    boolean existsByMembership_IdAndBranch_Id(
            UUID membershipId,
            UUID branchId
    );

    Optional<MembershipBranchAccess>
    findByMembership_IdAndBranch_Id(
            UUID membershipId,
            UUID branchId
    );

    List<MembershipBranchAccess>
    findByMembership_IdOrderByBranch_NameAsc(
            UUID membershipId
    );

    void deleteByMembership_Id(
            UUID membershipId
    );
}