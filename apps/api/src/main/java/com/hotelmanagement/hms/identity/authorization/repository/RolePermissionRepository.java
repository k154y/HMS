package com.hotelmanagement.hms.identity.authorization.repository;

import com.hotelmanagement.hms.identity.authorization.model.RolePermission;
import com.hotelmanagement.hms.identity.authorization.model.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository
        extends JpaRepository<RolePermission, RolePermissionId> {

    boolean existsByRole_IdAndPermission_Id(
            UUID roleId,
            UUID permissionId
    );

    List<RolePermission>
    findByRole_IdOrderByPermission_CodeAsc(
            UUID roleId
    );

    long deleteByRole_IdAndPermission_Id(
            UUID roleId,
            UUID permissionId
    );

    /**
     * Returns permissions provided by active roles currently assigned
     * to one membership within one hotel.
     *
     * Global user-account state and branch access are intentionally
     * evaluated separately by the final authorization layer.
     */
    @Query("""
            select distinct rp.permission.code
            from RolePermission rp,
                 MembershipRole mr
            where mr.membership.id = :membershipId
              and mr.hotelId = :hotelId
              and mr.role.id = rp.role.id
              and mr.role.active = true
            order by rp.permission.code
            """)
    List<String> findPermissionCodesForMembership(
            @Param("membershipId")
            UUID membershipId,

            @Param("hotelId")
            UUID hotelId
    );
}