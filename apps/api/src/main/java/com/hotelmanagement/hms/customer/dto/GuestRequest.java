package com.hotelmanagement.hms.customer.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record GuestRequest(@NotBlank @Size(max=200) String fullName,@PastOrPresent LocalDate dateOfBirth,
 @Pattern(regexp="[A-Z]{3}") String nationality,@Size(max=50) String phone) {}
