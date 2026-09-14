package com.hotelmanagement.hms.platform.onboarding.dto;

import com.hotelmanagement.hms.platform.dto.CreateHotelRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record OwnerSignupRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull @Size(max = 256) String password,
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 50) String phone,
        @Pattern(regexp = "en|fr|rw") String preferredLanguage,
        @Valid @NotNull CreateHotelRequest hotel) {
    @Override public String toString() { return "OwnerSignupRequest[REDACTED]"; }
}
