package com.hotelmanagement.hms.platform.dto;

import com.hotelmanagement.hms.platform.model.Branch;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BranchResponse(
        UUID id,
        UUID hotelId,
        String code,
        String name,
        String phone,
        String email,
        String address,
        String timezone,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static BranchResponse from(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getHotel().getId(),
                branch.getCode(),
                branch.getName(),
                branch.getPhone(),
                branch.getEmail(),
                branch.getAddress(),
                branch.getTimezone(),
                branch.isActive(),
                branch.getCreatedAt(),
                branch.getUpdatedAt()
        );
    }
}