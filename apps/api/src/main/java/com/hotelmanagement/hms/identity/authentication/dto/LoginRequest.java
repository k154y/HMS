package com.hotelmanagement.hms.identity.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(max = 256)
        String password
) {
    @Override public String toString() { return "LoginRequest[REDACTED]"; }
}
