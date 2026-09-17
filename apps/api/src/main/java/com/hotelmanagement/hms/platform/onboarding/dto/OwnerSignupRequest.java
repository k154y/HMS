package com.hotelmanagement.hms.platform.onboarding.dto;

import com.hotelmanagement.hms.platform.dto.CreateHotelRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OwnerSignupRequest(

        @NotBlank(
                message = "Owner email is required."
        )
        @Email(
                message = "Enter a valid owner email address."
        )
        @Size(
                max = 255,
                message = "Owner email must contain no more than 255 characters."
        )
        String email,

        @NotBlank(
                message = "Password is required."
        )
        @Size(
                min = 15,
                max = 128,
                message = "Password must contain between 15 and 128 characters."
        )
        String password,

        @NotBlank(
                message = "Owner full name is required."
        )
        @Size(
                max = 200,
                message = "Owner full name must contain no more than 200 characters."
        )
        String fullName,

        @Size(
                max = 50,
                message = "Owner phone number must contain no more than 50 characters."
        )
        String phone,

        @Pattern(
                regexp = "^(en|fr|rw)$",
                message = "Owner language must be English, French, or Kinyarwanda."
        )
        String preferredLanguage,

        @Valid
        @NotNull(
                message = "Hotel details are required."
        )
        CreateHotelRequest hotel
) {

    @Override
    public String toString() {
        return "OwnerSignupRequest[REDACTED]";
    }
}