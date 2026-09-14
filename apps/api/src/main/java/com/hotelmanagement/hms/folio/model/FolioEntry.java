package com.hotelmanagement.hms.folio.model;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="folio_entries")
public class FolioEntry extends com.hotelmanagement.hms.shared.model.BranchEntity {
    @Column(name="folio_id") private UUID folioId;
    @Column(name="amount",precision=19,scale=4) private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(name="kind") private EntryKind kind;
    @Column(name="source_id") private UUID sourceId;
    @Column(name="memo") private String memo;
    @Column(name="actor_id") private UUID actorId;
    protected FolioEntry() {}
    public static FolioEntry create(UUID hotelId, UUID branchId, UUID folioId, BigDecimal amount, EntryKind kind, UUID sourceId, String memo, UUID actorId) {
        var e=new FolioEntry();
        e.hotelId=hotelId;
        e.branchId=branchId;
        e.folioId=folioId;
        e.amount=amount;
        e.kind=kind;
        e.sourceId=sourceId;
        e.memo=memo;
        e.actorId=actorId;
        return e;
    }
    public UUID getFolioId() { return folioId; }
    public BigDecimal getAmount() { return amount; }
    public EntryKind getKind() { return kind; }
    public UUID getSourceId() { return sourceId; }
    public String getMemo() { return memo; }
    public UUID getActorId() { return actorId; }

}
