package com.hotelmanagement.hms.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateHotelRequest(

        @NotBlank
        @Size(max = 50)
        String code,

        @NotBlank
        @Size(max = 200)
        String legalName,

        @NotBlank
        @Size(max = 200)
        String displayName,

        @Size(max = 100)
        String tin,

        @Size(max = 50)
        String phone,

        @Email
        @Size(max = 255)
        String email,

        @Size(max = 2000)
        String address,

        @Pattern(
                regexp = "^[A-Z]{3}$",
                message = "Currency code must contain exactly three uppercase letters.")
        String currencyCode,

        @Size(max = 100)
        String timezone,

        @Pattern(
                regexp = "^(en|fr|rw)$",
                message = "Default language must be en, fr, or rw.")
        String defaultLanguage
) {
}