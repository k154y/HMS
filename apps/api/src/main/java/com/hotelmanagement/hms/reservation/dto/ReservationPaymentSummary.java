package com.hotelmanagement.hms.reservation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReservationPaymentSummary(
        UUID reservationId,
        String reference,
        UUID folioId,
        String currency,
        BigDecimal reservationTotal,
        BigDecimal advancePaid,
        BigDecimal remaining,
        String paymentStatus) {
}