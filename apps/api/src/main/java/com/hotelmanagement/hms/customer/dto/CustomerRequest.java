package com.hotelmanagement.hms.customer.dto;
import com.hotelmanagement.hms.customer.model.CustomerKind;
import jakarta.validation.constraints.*;
public record CustomerRequest(@NotBlank @Size(max=100) String code,@NotNull CustomerKind kind,
 @NotBlank @Size(max=200) String name,@Email @Size(max=255) String email,@Size(max=50) String phone,
 @Size(max=2000) String address,@Size(max=100) String taxNumber,boolean active) {}
