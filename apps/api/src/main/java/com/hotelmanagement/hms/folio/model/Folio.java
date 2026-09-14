package com.hotelmanagement.hms.folio.model;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="folios")
public class Folio extends com.hotelmanagement.hms.shared.model.BranchEntity {
    @Column(name="customer_id") private UUID customerId;
    @Column(name="currency") private String currency;
    @Enumerated(EnumType.STRING)
    @Column(name="status") private FolioStatus status;
    protected Folio() {}
    public static Folio create(UUID hotelId, UUID branchId, UUID customerId, String currency, FolioStatus status) {
        var e=new Folio();
        e.hotelId=hotelId;
        e.branchId=branchId;
        e.customerId=customerId;
        e.currency=currency;
        e.status=status;
        return e;
    }
    public UUID getCustomerId() { return customerId; }
    public String getCurrency() { return currency; }
    public FolioStatus getStatus() { return status; }
 public void close() { if(status!=FolioStatus.OPEN) throw new IllegalStateException("Folio is not open."); status=FolioStatus.CLOSED; }
 public void credit() { if(status!=FolioStatus.OPEN) throw new IllegalStateException("Folio is not open.");status=FolioStatus.CREDIT; }
 public void reopenForRefund(){if(status==FolioStatus.CLOSED)status=FolioStatus.OPEN;}
}
