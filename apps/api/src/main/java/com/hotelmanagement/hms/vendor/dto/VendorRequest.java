package com.hotelmanagement.hms.vendor.dto;
import jakarta.validation.constraints.*;
public record VendorRequest(@NotBlank @Size(max=100) String code,@NotBlank @Size(max=200) String name,
 @Email @Size(max=255) String email,@Size(max=50) String phone,@Size(max=2000) String address,@Min(0) int paymentTermsDays,boolean active) {}
