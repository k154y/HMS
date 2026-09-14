package com.hotelmanagement.hms.customer.dto;
import com.hotelmanagement.hms.customer.model.*;
import java.util.UUID;
public record CustomerResponse(UUID id,String code,CustomerKind kind,String name,String email,String phone,String address,String taxNumber,boolean active) {
 public static CustomerResponse from(Customer e) { return new CustomerResponse(e.getId(),e.getCode(),e.getKind(),e.getName(),e.getEmail(),e.getPhone(),e.getAddress(),e.getTaxNumber(),e.getActive()); }
}
