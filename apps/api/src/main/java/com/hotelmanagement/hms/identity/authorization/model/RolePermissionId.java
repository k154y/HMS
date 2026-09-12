package com.hotelmanagement.hms.identity.authorization.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RolePermissionId implements Serializable {

    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "permission_id")
    private UUID permissionId;

    protected RolePermissionId() {
        // Required by JPA.
    }

    public RolePermissionId(
            UUID roleId,
            UUID permissionId) {

        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public UUID getPermissionId() {
        return permissionId;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (!(other instanceof RolePermissionId that)) {
            return false;
        }

        return Objects.equals(
                roleId,
                that.roleId
        ) && Objects.equals(
                permissionId,
                that.permissionId
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                roleId,
                permissionId
        );
    }
}