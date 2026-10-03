package com.hotelmanagement.hms.identity.authentication.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(@NotNull @Size(max = 256) String currentPassword,
                                    @NotNull @Size(max = 256) String newPassword, @Size(max = 256) String confirmPassword) {
    public ChangePasswordRequest(String currentPassword, String newPassword) { this(currentPassword, newPassword, null); }
    @Override public String toString() { return "ChangePasswordRequest[REDACTED]"; }
}
