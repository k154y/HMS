package com.hotelmanagement.hms.reservation.dto;
import com.hotelmanagement.hms.reservation.model.*; import java.time.*; import java.util.*;
public record ReservationResponse(UUID id,String reference,UUID customerId,UUID folioId,LocalDate checkIn,LocalDate checkOut,int adults,int children,ReservationStatus status,List<UUID> roomIds,String notes) {
 public static ReservationResponse from(Reservation r,List<ReservationRoom> rooms){return new ReservationResponse(r.getId(),r.getReservationReference(),r.getBookingCustomerId(),r.getFolioId(),r.getCheckIn(),r.getCheckOut(),r.getAdults(),r.getChildren(),r.getStatus(),rooms.stream().map(ReservationRoom::getRoomId).toList(),r.getNotes());}
}
