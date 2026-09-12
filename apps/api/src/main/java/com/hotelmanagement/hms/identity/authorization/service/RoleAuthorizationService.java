package com.hotelmanagement.hms.identity.authorization.service;

import com.hotelmanagement.hms.identity.authorization.model.MembershipRole;
import com.hotelmanagement.hms.identity.authorization.model.Permission;
import com.hotelmanagement.hms.identity.authorization.model.Role;
import com.hotelmanagement.hms.identity.authorization.model.RolePermission;
import com.hotelmanagement.hms.identity.authorization.repository.MembershipRoleRepository;
import com.hotelmanagement.hms.identity.authorization.repository.PermissionRepository;
import com.hotelmanagement.hms.identity.authorization.repository.RolePermissionRepository;
import com.hotelmanagement.hms.identity.authorization.repository.RoleRepository;
import com.hotelmanagement.hms.identity.membership.model.HotelMembership;
import com.hotelmanagement.hms.identity.membership.model.HotelMembershipStatus;
import com.hotelmanagement.hms.identity.membership.repository.HotelMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class RoleAuthorizationService {

    private final RoleRepository roleRepository;

    private final PermissionRepository
            permissionRepository;

    private final RolePermissionRepository
            rolePermissionRepository;

    private final HotelMembershipRepository
            membershipRepository;

    private final MembershipRoleRepository
            membershipRoleRepository;

    public RoleAuthorizationService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            HotelMembershipRepository membershipRepository,
            MembershipRoleRepository membershipRoleRepository) {

        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleRepository = membershipRoleRepository;
    }

    /**
     * Grants one permission to a role belonging to the supplied hotel.
     */
    @Transactional
    public void assignPermissionToRole(
            UUID hotelId,
            UUID roleId,
            String permissionCode) {

        Role role = getRole(
                hotelId,
                roleId
        );

        Permission permission =
                getPermission(permissionCode);

        if (rolePermissionRepository
                .existsByRole_IdAndPermission_Id(
                        role.getId(),
                        permission.getId())) {

            throw new IllegalStateException(
                    "The role already has permission "
                            + permission.getCode() + ".");
        }

        OffsetDateTime now =
                OffsetDateTime.now(
                        ZoneOffset.UTC);

        RolePermission rolePermission =
                RolePermission.create(
                        role,
                        permission,
                        now
                );

        rolePermissionRepository.saveAndFlush(
                rolePermission);
    }

    /**
     * Removes one permission from a hotel role.
     */
    @Transactional
    public void removePermissionFromRole(
            UUID hotelId,
            UUID roleId,
            String permissionCode) {

        Role role = getRole(
                hotelId,
                roleId
        );

        Permission permission =
                getPermission(permissionCode);

        long deleted =
                rolePermissionRepository
                        .deleteByRole_IdAndPermission_Id(
                                role.getId(),
                                permission.getId()
                        );

        if (deleted == 0) {
            throw new IllegalArgumentException(
                    "The role does not currently have permission "
                            + permission.getCode() + ".");
        }
    }

    /**
     * Assigns a hotel-scoped role to a hotel membership.
     *
     * Tenant-scoped lookups ensure that both records belong
     * to the same hotel before the assignment is created.
     */
    @Transactional
    public void assignRoleToMembership(
            UUID hotelId,
            UUID membershipId,
            UUID roleId) {

        HotelMembership membership =
                getMembership(
                        hotelId,
                        membershipId);

        Role role =
                getRole(
                        hotelId,
                        roleId);

        if (membership.getStatus()
                == HotelMembershipStatus.REVOKED) {

            throw new IllegalStateException(
                    "Roles cannot be assigned to a revoked "
                            + "hotel membership.");
        }

        if (!role.isActive()) {
            throw new IllegalStateException(
                    "An inactive role cannot be assigned.");
        }

        if (membershipRoleRepository
                .existsByMembership_IdAndRole_Id(
                        membershipId,
                        roleId)) {

            throw new IllegalStateException(
                    "The membership already has this role.");
        }

        OffsetDateTime now =
                OffsetDateTime.now(
                        ZoneOffset.UTC);

        MembershipRole membershipRole =
                MembershipRole.create(
                        membership,
                        role,
                        now
                );

        membershipRoleRepository.saveAndFlush(
                membershipRole);
    }

    /**
     * Removes a role assignment while preserving the user,
     * membership, role, and all financial/operational history.
     */
    @Transactional
    public void removeRoleFromMembership(
            UUID hotelId,
            UUID membershipId,
            UUID roleId) {

        /*
         * These tenant-scoped lookups prevent IDs from another
         * hotel being used in this operation.
         */
        getMembership(
                hotelId,
                membershipId);

        getRole(
                hotelId,
                roleId);

        MembershipRole membershipRole =
                membershipRoleRepository
                        .findByMembership_IdAndRole_Id(
                                membershipId,
                                roleId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "The membership does not "
                                                + "have this role."));

        membershipRoleRepository.delete(
                membershipRole);
    }

    /**
     * Returns the capabilities supplied by active roles assigned
     * to one active hotel membership.
     *
     * This is not yet the final HTTP authorization decision because
     * global account status and branch access are separate concerns.
     */
    @Transactional(readOnly = true)
    public Set<String> getMembershipPermissionCodes(
            UUID hotelId,
            UUID membershipId) {

        HotelMembership membership =
                getMembership(
                        hotelId,
                        membershipId);

        if (membership.getStatus()
                != HotelMembershipStatus.ACTIVE) {

            return Set.of();
        }

        List<String> permissionCodes =
                rolePermissionRepository
                        .findPermissionCodesForMembership(
                                membershipId,
                                hotelId
                        );

        return new LinkedHashSet<>(
                permissionCodes);
    }

    private Role getRole(
            UUID hotelId,
            UUID roleId) {

        return roleRepository
                .findByIdAndHotel_Id(
                        roleId,
                        hotelId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Role not found in this hotel: "
                                        + roleId));
    }

    private HotelMembership getMembership(
            UUID hotelId,
            UUID membershipId) {

        return membershipRepository
                .findByIdAndHotel_Id(
                        membershipId,
                        hotelId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Membership not found in this hotel: "
                                        + membershipId));
    }

    private Permission getPermission(
            String permissionCode) {

        String normalizedCode =
                normalizePermissionCode(
                        permissionCode);

        return permissionRepository
                .findByCode(normalizedCode)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown permission: "
                                        + normalizedCode));
    }

    private String normalizePermissionCode(
            String permissionCode) {

        if (permissionCode == null
                || permissionCode.isBlank()) {

            throw new IllegalArgumentException(
                    "Permission code is required.");
        }

        return permissionCode
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}