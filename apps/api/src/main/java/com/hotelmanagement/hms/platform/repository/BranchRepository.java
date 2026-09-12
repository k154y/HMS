package com.hotelmanagement.hms.platform.repository;

import com.hotelmanagement.hms.platform.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository
        extends JpaRepository<Branch, UUID> {

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