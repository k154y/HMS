package com.hotelmanagement.hms.identity.authorization.service;

import com.hotelmanagement.hms.identity.authorization.model.DefaultHotelRole;
import com.hotelmanagement.hms.identity.authorization.model.Permission;
import com.hotelmanagement.hms.identity.authorization.model.PermissionCode;
import com.hotelmanagement.hms.identity.authorization.model.Role;
import com.hotelmanagement.hms.identity.authorization.model.RolePermission;
import com.hotelmanagement.hms.identity.authorization.repository.PermissionRepository;
import com.hotelmanagement.hms.identity.authorization.repository.RolePermissionRepository;
import com.hotelmanagement.hms.identity.authorization.repository.RoleRepository;
import com.hotelmanagement.hms.platform.model.Hotel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DefaultRoleProvisioningService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public DefaultRoleProvisioningService(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository) {

        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * Creates the standard HMS tenant roles for a newly created hotel.
     *
     * The operation is intentionally idempotent at role level:
     * if a role with the standard code already exists, it is not
     * overwritten. This prevents a later provisioning call from
     * destroying hotel-specific role customization.
     */
    @Transactional
    public void provisionDefaultRoles(
            Hotel hotel) {

        if (hotel == null || hotel.getId() == null) {
            throw new IllegalArgumentException(
                    "A persisted hotel is required "
                            + "before roles can be provisioned.");
        }

        Map<String, Permission> permissionsByCode =
                loadPermissionCatalog();

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        for (DefaultHotelRole defaultRole
                : DefaultHotelRole.values()) {

            if (roleRepository
                    .existsByHotel_IdAndCode(
                            hotel.getId(),
                            defaultRole.getCode())) {

                continue;
            }

            Role role = Role.create(
                    hotel,
                    defaultRole.getCode(),
                    defaultRole.getDisplayName(),
                    defaultRole.getDescription(),
                    true,
                    now
            );

            Role savedRole =
                    roleRepository.saveAndFlush(role);

            for (PermissionCode permissionCode
                    : defaultRole.getPermissions()) {

                Permission permission =
                        permissionsByCode.get(
                                permissionCode.name());

                if (permission == null) {
                    throw new IllegalStateException(
                            "Permission catalog is missing: "
                                    + permissionCode.name());
                }

                RolePermission rolePermission =
                        RolePermission.create(
                                savedRole,
                                permission,
                                now
                        );

                rolePermissionRepository.save(
                        rolePermission);
            }

            rolePermissionRepository.flush();
        }
    }

    private Map<String, Permission> loadPermissionCatalog() {

        List<Permission> permissions =
                permissionRepository
                        .findAllByOrderByCodeAsc();

        Map<String, Permission> permissionsByCode =
                new HashMap<>();

        for (Permission permission : permissions) {
            permissionsByCode.put(
                    permission.getCode(),
                    permission
            );
        }

        Set<PermissionCode> missing =
                EnumSet.noneOf(
                        PermissionCode.class);

        for (PermissionCode required
                : PermissionCode.values()) {

            if (!permissionsByCode.containsKey(
                    required.name())) {

                missing.add(required);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Permission catalog is incomplete. Missing: "
                            + missing);
        }

        return permissionsByCode;
    }
}