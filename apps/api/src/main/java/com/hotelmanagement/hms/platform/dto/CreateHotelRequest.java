package com.hotelmanagement.hms.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateHotelRequest(

        @NotBlank(
                message = "Hotel code is required."
        )
        @Size(
                max = 50,
                message = "Hotel code must contain no more than 50 characters."
        )
        String code,

        @NotBlank(
                message = "Hotel legal name is required."
        )
        @Size(
                max = 200,
                message = "Hotel legal name must contain no more than 200 characters."
        )
        String legalName,

        @NotBlank(
                message = "Hotel display name is required."
        )
        @Size(
                max = 200,
                message = "Hotel display name must contain no more than 200 characters."
        )
        String displayName,

        @Size(
                max = 100,
                message = "TIN must contain no more than 100 characters."
        )
        String tin,

        @Size(
                max = 50,
                message = "Hotel phone number must contain no more than 50 characters."
        )
        String phone,

        @Email(
                message = "Enter a valid hotel email address."
        )
        @Size(
                max = 255,
                message = "Hotel email must contain no more than 255 characters."
        )
        String email,

        @Size(
                max = 2000,
                message = "Hotel address must contain no more than 2000 characters."
        )
        String address,

        @Pattern(
                regexp = "^[A-Z]{3}$",
                message = "Currency code must contain exactly three uppercase letters."
        )
        String currencyCode,

        @Size(
                max = 100,
                message = "Timezone must contain no more than 100 characters."
        )
        String timezone,

        @Pattern(
                regexp = "^(en|fr|rw)$",
                message = "Default language must be English, French, or Kinyarwanda."
        )
        String defaultLanguage
) {
}