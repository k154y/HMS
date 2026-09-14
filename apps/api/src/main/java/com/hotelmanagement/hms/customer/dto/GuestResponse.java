package com.hotelmanagement.hms.customer.dto;
import com.hotelmanagement.hms.customer.model.Guest;
import java.util.UUID;
import java.time.LocalDate;
public record GuestResponse(UUID id,String fullName,LocalDate dateOfBirth,String nationality,String phone) {
 public static GuestResponse from(Guest e) { return new GuestResponse(e.getId(),e.getFullName(),e.getDateOfBirth(),e.getNationality(),e.getPhone()); }
}
