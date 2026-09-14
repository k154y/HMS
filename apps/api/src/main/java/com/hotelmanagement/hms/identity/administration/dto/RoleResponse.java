package com.hotelmanagement.hms.identity.administration.dto;
import com.hotelmanagement.hms.identity.authorization.model.Role;
import java.util.UUID;
public record RoleResponse(UUID id, String code, String name, String description, boolean active, boolean systemDefined) {
    public static RoleResponse from(Role r) {
        return new RoleResponse(r.getId(), r.getCode(), r.getName(), r.getDescription(), r.isActive(), r.isSystemDefined());
    }
}
