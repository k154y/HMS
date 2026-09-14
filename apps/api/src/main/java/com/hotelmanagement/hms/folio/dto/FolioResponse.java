package com.hotelmanagement.hms.folio.dto;
import com.hotelmanagement.hms.folio.model.*;
import java.util.UUID;
import java.math.BigDecimal;
public record FolioResponse(UUID id,UUID customerId,String currency,FolioStatus status,BigDecimal balance) {
 public static FolioResponse from(Folio f,BigDecimal balance) { return new FolioResponse(f.getId(),f.getCustomerId(),f.getCurrency(),f.getStatus(),balance); }
}
