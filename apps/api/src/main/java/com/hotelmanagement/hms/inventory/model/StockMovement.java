package com.hotelmanagement.hms.inventory.model;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="stock_movements")
public class StockMovement extends com.hotelmanagement.hms.shared.model.BranchEntity {
    @Column(name="product_id") private UUID productId;
    @Column(name="quantity",precision=19,scale=4) private BigDecimal quantity;
    @Column(name="unit") private String unit;
    @Enumerated(EnumType.STRING)
    @Column(name="kind") private MovementKind kind;
    @Column(name="source_id") private UUID sourceId;
    @Column(name="actor_id") private UUID actorId;
    @Column(name="reason") private String reason;
    protected StockMovement() {}
    public static StockMovement create(UUID hotelId, UUID branchId, UUID productId, BigDecimal quantity, String unit, MovementKind kind, UUID sourceId, UUID actorId, String reason) {
        var e=new StockMovement();
        e.hotelId=hotelId;
        e.branchId=branchId;
        e.productId=productId;
        e.quantity=quantity;
        e.unit=unit;
        e.kind=kind;
        e.sourceId=sourceId;
        e.actorId=actorId;
        e.reason=reason;
        return e;
    }
    public UUID getProductId() { return productId; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public MovementKind getKind() { return kind; }
    public UUID getSourceId() { return sourceId; }
    public UUID getActorId() { return actorId; }
    public String getReason() { return reason; }

}
