package com.hotelmanagement.hms.audit.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
@org.hibernate.annotations.Immutable
public class AuditEvent {
    @Id private UUID id;
    @Column(name = "hotel_id") private UUID hotelId;
    @Column(name = "branch_id") private UUID branchId;
    @Column(name = "actor_user_id") private UUID actorUserId;
    @Column(nullable = false, length = 100) private String action;
    @Column(name = "entity_type", nullable = false, length = 100) private String entityType;
    @Column(name = "entity_id", nullable = false) private UUID entityId;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "request_id", length = 64) private String requestId;
    @Column(name="old_value",columnDefinition="text") private String oldValue;
    @Column(name="new_value",columnDefinition="text") private String newValue;
    @Column(length=1000) private String reason;
    @Column(name="approval_status",length=30) private String approvalStatus;
    @Column(length=500) private String device;
    @Column(name="remote_address",length=100) private String remoteAddress;
    public AuditEvent details(String before,String after,String reason,String status,String device,String remoteAddress){this.oldValue=before;this.newValue=after;this.reason=reason;this.approvalStatus=status;this.device=device;this.remoteAddress=remoteAddress;return this;}
    public String getOldValue(){return oldValue;} public String getNewValue(){return newValue;}
    public String getReason(){return reason;} public String getApprovalStatus(){return approvalStatus;}
    public String getDevice(){return device;} public String getRemoteAddress(){return remoteAddress;}
    protected AuditEvent() {}
    public AuditEvent(UUID hotelId, UUID branchId, UUID actor, String action, String type, UUID entity, String requestId) {
        this.id = UUID.randomUUID(); this.hotelId = hotelId; this.branchId = branchId; this.actorUserId = actor;
        this.action = action; this.entityType = type; this.entityId = entity; this.requestId = requestId;
        this.occurredAt = Instant.now();
    }
    public UUID getId() { return id; }
    public UUID getHotelId() { return hotelId; }
    public UUID getBranchId() { return branchId; }
    public UUID getActorUserId() { return actorUserId; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getRequestId() { return requestId; }
}
