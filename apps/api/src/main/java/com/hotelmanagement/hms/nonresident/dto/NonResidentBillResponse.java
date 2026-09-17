package com.hotelmanagement.hms.nonresident.dto;

import com.hotelmanagement.hms.customer.model.CustomerKind;
import com.hotelmanagement.hms.nonresident.model.NonResidentBillStatus;
import com.hotelmanagement.hms.nonresident.model.NonResidentBillType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NonResidentBillResponse(

        UUID id,

        UUID folioId,

        UUID customerId,

        String customerName,

        CustomerKind customerKind,

        String reference,

        NonResidentBillType billType,

        String tableReference,

        String notes,

        NonResidentBillStatus status,

        BigDecimal total,

        BigDecimal paid,

        BigDecimal balance,

        OffsetDateTime createdAt
) {
}