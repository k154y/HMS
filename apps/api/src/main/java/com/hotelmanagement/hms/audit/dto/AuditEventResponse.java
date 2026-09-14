package com.hotelmanagement.hms.audit.dto;
import com.hotelmanagement.hms.audit.model.AuditEvent;
import java.time.Instant;
import java.util.UUID;
public record AuditEventResponse(UUID id, UUID hotelId, UUID branchId, UUID actorUserId, String action,
        String entityType, UUID entityId, Instant occurredAt, String requestId,String oldValue,String newValue,String reason,String approvalStatus,String device,String remoteAddress) {
    public static AuditEventResponse from(AuditEvent e) {
        return new AuditEventResponse(e.getId(), e.getHotelId(), e.getBranchId(), e.getActorUserId(),
                e.getAction(), e.getEntityType(), e.getEntityId(), e.getOccurredAt(), e.getRequestId(),e.getOldValue(),e.getNewValue(),e.getReason(),e.getApprovalStatus(),e.getDevice(),e.getRemoteAddress());
    }
}
