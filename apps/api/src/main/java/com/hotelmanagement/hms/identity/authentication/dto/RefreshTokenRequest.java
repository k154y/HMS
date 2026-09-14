package com.hotelmanagement.hms.identity.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(

        @NotBlank
        @Size(max = 500)
        String refreshToken
) {
    @Override public String toString() { return "RefreshTokenRequest[REDACTED]"; }
}
