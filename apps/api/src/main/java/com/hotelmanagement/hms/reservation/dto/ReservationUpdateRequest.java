package com.hotelmanagement.hms.reservation.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReservationUpdateRequest(
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut) {
}
