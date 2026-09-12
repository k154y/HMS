package com.hotelmanagement.hms.identity.authorization.repository;

import com.hotelmanagement.hms.identity.authorization.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository
        extends JpaRepository<Role, UUID> {

    Optional<Role> findByIdAndHotel_Id(
            UUID roleId,
            UUID hotelId
    );

    Optional<Role> findByHotel_IdAndCode(
            UUID hotelId,
            String code
    );

    boolean existsByHotel_IdAndCode(
            UUID hotelId,
            String code
    );

    List<Role> findByHotel_IdOrderByNameAsc(
            UUID hotelId
    );

    List<Role> findByHotel_IdAndActiveTrueOrderByNameAsc(
            UUID hotelId
    );
}