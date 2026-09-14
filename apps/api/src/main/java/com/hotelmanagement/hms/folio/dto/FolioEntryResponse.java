package com.hotelmanagement.hms.folio.dto;
import com.hotelmanagement.hms.folio.model.*;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
public record FolioEntryResponse(UUID id,BigDecimal amount,EntryKind kind,UUID sourceId,String memo,UUID actorId,Instant createdAt) {
 public static FolioEntryResponse from(FolioEntry e) { return new FolioEntryResponse(e.getId(),e.getAmount(),e.getKind(),e.getSourceId(),e.getMemo(),e.getActorId(),e.getCreatedAt()); }
}
