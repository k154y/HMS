package com.hotelmanagement.hms.identity.authentication.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(@NotNull @Size(max = 256) String currentPassword,
                                    @NotNull @Size(max = 256) String newPassword) {
    @Override public String toString() { return "ChangePasswordRequest[REDACTED]"; }
}
