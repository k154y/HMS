package com.hotelmanagement.hms.identity.administration.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
// Supply either an existing global identity or new-account details, never both.
public record StaffRequest(UUID existingUserId, @Email @Size(max=255) String email,
        @Size(max=256) String password, @Size(max=200) String fullName, @Size(max=50) String phone,
        @Pattern(regexp="en|fr|rw") String preferredLanguage, boolean allBranches) {
    @Override public String toString() { return "StaffRequest[REDACTED]"; }
}
