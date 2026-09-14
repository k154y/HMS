package com.hotelmanagement.hms.platform.repository;

import com.hotelmanagement.hms.platform.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository
        extends JpaRepository<Branch, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from Branch b where b.id=:id and b.hotel.id=:hotel")
    Optional<Branch> findLockedInHotel(UUID id, UUID hotel);

    @org.springframework.data.jpa.repository.Query("""
        select b from Branch b where b.hotel.id=:hotel and exists (
          select m.id from HotelMembership m where m.hotel.id=:hotel and m.user.id=:actor
          and (m.allBranches=true or exists (
            select a.id from MembershipBranchAccess a where a.membership.id=m.id and a.branch.id=b.id)))
        """)
    org.springframework.data.domain.Page<Branch> findAccessible(UUID hotel, UUID actor, org.springframework.data.domain.Pageable page);

    boolean existsByHotel_IdAndCode(
            UUID hotelId,
            String code
    );

    Optional<Branch> findByIdAndHotel_Id(
            UUID branchId,
            UUID hotelId
    );

    Optional<Branch> findByHotel_IdAndCode(
            UUID hotelId,
            String code
    );

    List<Branch> findByHotel_IdOrderByNameAsc(
            UUID hotelId
    );

    List<Branch> findByHotel_IdAndActiveTrueOrderByNameAsc(
            UUID hotelId
    );
}
