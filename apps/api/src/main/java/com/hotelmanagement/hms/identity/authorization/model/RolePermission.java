package com.hotelmanagement.hms.identity.authorization.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "role_permissions")
public class RolePermission {

    @EmbeddedId
    private RolePermissionId id;

    @MapsId("roleId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "role_id",
            nullable = false
    )
    private Role role;

    @MapsId("permissionId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "permission_id",
            nullable = false
    )
    private Permission permission;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    protected RolePermission() {
        // Required by JPA.
    }

    public static RolePermission create(
            Role role,
            Permission permission,
            OffsetDateTime createdAt) {

        RolePermission rolePermission =
                new RolePermission();

        rolePermission.id =
                new RolePermissionId(
                        role.getId(),
                        permission.getId()
                );

        rolePermission.role = role;
        rolePermission.permission = permission;
        rolePermission.createdAt = createdAt;

        return rolePermission;
    }

    public RolePermissionId getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public Permission getPermission() {
        return permission;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}