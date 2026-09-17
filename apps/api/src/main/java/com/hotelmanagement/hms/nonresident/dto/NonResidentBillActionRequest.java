package com.hotelmanagement.hms.nonresident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requires staff to explain sensitive bill actions.
 *
 * Cancellation and voiding must never happen silently because
 * those actions affect operational and financial audit trails.
 */
public record NonResidentBillActionRequest(

        @NotBlank(
                message =
                        "Reason is required."
        )
        @Size(
                max = 1000,
                message =
                        "Reason must not exceed 1000 characters."
        )
        String reason
) {
}