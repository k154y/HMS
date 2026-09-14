package com.hotelmanagement.hms.vendor.dto;
import com.hotelmanagement.hms.vendor.model.*;
import java.util.UUID;
import java.math.BigDecimal;
public record VendorResponse(UUID id,String code,String name,String email,String phone,String address,int paymentTermsDays,boolean active) {
 public static VendorResponse from(Vendor e){return new VendorResponse(e.getId(),e.getCode(),e.getName(),e.getEmail(),e.getPhone(),e.getAddress(),e.getPaymentTermsDays(),e.getActive());}
}
