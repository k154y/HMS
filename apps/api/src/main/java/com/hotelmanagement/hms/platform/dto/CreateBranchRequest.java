package com.hotelmanagement.hms.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(

        @NotBlank
        @Size(max = 50)
        String code,

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 50)
        String phone,

        @Email
        @Size(max = 255)
        String email,

        @Size(max = 2000)
        String address,

        @Size(max = 100)
        String timezone
) {
}