package com.hotelmanagement.hms.nonresident.dto;

import com.hotelmanagement.hms.nonresident.model.NonResidentBillType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Opens a non-resident bill.
 *
 * customerId is optional:
 *
 * - supplied customerId:
 *   use an existing individual/company/NGO/etc.
 *
 * - null customerId:
 *   use the hotel's system Walk-in customer.
 */
public record NonResidentBillRequest(

        UUID customerId,

        @NotNull(
                message =
                        "Bill type is required."
        )
        NonResidentBillType billType,

        @Size(
                max = 100,
                message =
                        "Table or reference must not exceed 100 characters."
        )
        String tableReference,

        @Size(
                max = 1000,
                message =
                        "Notes must not exceed 1000 characters."
        )
        String notes
) {
}