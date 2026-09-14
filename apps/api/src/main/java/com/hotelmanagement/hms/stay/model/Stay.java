package com.hotelmanagement.hms.stay.model;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="stays")
public class Stay extends com.hotelmanagement.hms.shared.model.BranchEntity {
    @Column(name="reservation_id") private UUID reservationId;
    @Column(name="room_id") private UUID roomId;
    @Column(name="folio_id") private UUID folioId;
    @Column(name="checked_in_at") private Instant checkedInAt;
    @Column(name="checked_out_at") private Instant checkedOutAt;
    @Column(name="checked_in_by") private UUID checkedInBy;
    @Column(name="checked_out_by") private UUID checkedOutBy;
    protected Stay() {}
    public static Stay create(UUID hotelId, UUID branchId, UUID reservationId, UUID roomId, UUID folioId, Instant checkedInAt, Instant checkedOutAt, UUID checkedInBy, UUID checkedOutBy) {
        var e=new Stay();
        e.hotelId=hotelId;
        e.branchId=branchId;
        e.reservationId=reservationId;
        e.roomId=roomId;
        e.folioId=folioId;
        e.checkedInAt=checkedInAt;
        e.checkedOutAt=checkedOutAt;
        e.checkedInBy=checkedInBy;
        e.checkedOutBy=checkedOutBy;
        return e;
    }
    public UUID getReservationId() { return reservationId; }
    public UUID getRoomId() { return roomId; }
    public UUID getFolioId() { return folioId; }
    public Instant getCheckedInAt() { return checkedInAt; }
    public Instant getCheckedOutAt() { return checkedOutAt; }
    public UUID getCheckedInBy() { return checkedInBy; }
    public UUID getCheckedOutBy() { return checkedOutBy; }
 public void checkOut(UUID actor) { if(checkedOutAt!=null)throw new IllegalStateException("Stay is already closed.");checkedOutAt=Instant.now();checkedOutBy=actor; }
}
