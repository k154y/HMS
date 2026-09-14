package com.hotelmanagement.hms.reservation.model;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="reservation_guests")
public class ReservationGuest extends com.hotelmanagement.hms.shared.model.BranchEntity {
    @Column(name="allocation_id") private UUID allocationId;
    @Column(name="guest_id") private UUID guestId;
    protected ReservationGuest() {}
    public static ReservationGuest create(UUID hotelId, UUID branchId, UUID allocationId, UUID guestId) {
        var e=new ReservationGuest();
        e.hotelId=hotelId;
        e.branchId=branchId;
        e.allocationId=allocationId;
        e.guestId=guestId;
        return e;
    }
    public UUID getAllocationId() { return allocationId; }
    public UUID getGuestId() { return guestId; }

}
