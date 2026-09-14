package com.hotelmanagement.hms.inventory.dto;
import com.hotelmanagement.hms.inventory.model.*;
import java.math.BigDecimal;
import java.util.UUID;
import java.time.Instant;
public record MovementResponse(UUID id,UUID productId,BigDecimal quantity,String unit,MovementKind kind,UUID sourceId,String reason,UUID actorId,Instant createdAt) {
 public static MovementResponse from(StockMovement m){return new MovementResponse(m.getId(),m.getProductId(),m.getQuantity(),m.getUnit(),m.getKind(),m.getSourceId(),m.getReason(),m.getActorId(),m.getCreatedAt());}
}
