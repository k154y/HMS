package com.hotelmanagement.hms.reservation.dto;
import jakarta.validation.constraints.*; import java.time.*; import java.util.*; import java.math.BigDecimal;
public record ReservationRequest(@NotNull UUID bookingCustomerId,@NotNull LocalDate checkIn,@NotNull LocalDate checkOut,@Min(1) int adults,@Min(0) int children,
 @NotEmpty @Size(max=20) List<@NotNull RoomBooking> rooms,@Size(max=2000) String notes) {
 public ReservationRequest(UUID customerId, LocalDate checkIn, LocalDate checkOut, List<RoomBooking> rooms, String notes) {
  this(customerId, checkIn, checkOut, rooms.stream().mapToInt(RoomBooking::adults).sum(), rooms.stream().mapToInt(RoomBooking::children).sum(), rooms, notes);
 }
 public record RoomBooking(@NotNull UUID roomId,@Min(1) int adults,@Min(0) int children, @DecimalMin("0") BigDecimal rate, List<UUID> guestIds) {}
 // Backward-compatible alias for clients using the descriptive name.
 public record RoomAllocation(@NotNull UUID roomId,@NotNull @DecimalMin("0") @Digits(integer=15,fraction=4) BigDecimal rate,@Min(1) int adults,@Min(0) int children){}
}
