package com.hotelmanagement.hms.reservation.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReservationUpdateRequest(
        @NotNull(message="Check-in date is required.") LocalDate checkIn,
        @NotNull(message="Check-out date is required.") LocalDate checkOut) {
}
