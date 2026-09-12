package com.hotelmanagement.hms.identity.authorization.repository;

import com.hotelmanagement.hms.identity.authorization.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository
        extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(
            String code
    );

    boolean existsByCode(
            String code
    );

    List<Permission> findByCodeIn(
            Collection<String> codes
    );

    List<Permission> findAllByOrderByCodeAsc();
}