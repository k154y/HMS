package com.hotelmanagement.hms.room.model;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="rooms")
public class Room extends com.hotelmanagement.hms.shared.model.BranchEntity {
    @Column(name="room_type_id") private UUID roomTypeId;
    @Column(name="code") private String code;
    @Column(name="floor") private String floor;
    @Column(name="beds") private int beds;
    @Column(name="bed_type") private String bedType;
    @Column(name="bed_dimensions") private String bedDimensions;
    @Column(name="adults") private int adults;
    @Column(name="children") private int children;
    @Column(name="active") private boolean active;
    @Enumerated(EnumType.STRING)
    @Column(name="housekeeping") private HousekeepingState housekeeping;
    @Enumerated(EnumType.STRING)
    @Column(name="operational") private RoomOperationalState operational;
    @Column(name="nightly_rate",precision=19,scale=4) private BigDecimal nightlyRate;
    protected Room() {}
    public static Room create(UUID hotelId, UUID branchId, UUID roomTypeId, String code, String floor, int beds, String bedType, String bedDimensions, int adults, int children, boolean active, HousekeepingState housekeeping, RoomOperationalState operational) {
        var e=new Room();
        e.hotelId=hotelId;
        e.branchId=branchId;
        e.roomTypeId=roomTypeId;
        e.code=code;
        e.floor=floor;
        e.beds=beds;
        e.bedType=bedType;
        e.bedDimensions=bedDimensions;
        e.adults=adults;
        e.children=children;
        e.active=active;
        e.housekeeping=housekeeping;
        e.operational=operational;
        return e;
    }
    public UUID getRoomTypeId() { return roomTypeId; }
    public String getCode() { return code; }
    public String getFloor() { return floor; }
    public int getBeds() { return beds; }
    public String getBedType() { return bedType; }
    public String getBedDimensions() { return bedDimensions; }
    public int getAdults() { return adults; }
    public int getChildren() { return children; }
    public boolean getActive() { return active; }
    public HousekeepingState getHousekeeping() { return housekeeping; }
    public RoomOperationalState getOperational() { return operational; }
 public BigDecimal getNightlyRate(){return nightlyRate;}
 public void update(UUID type,String code,String floor,int beds,String bedType,String dimensions,int adults,int children,BigDecimal rate){roomTypeId=type;this.code=code;this.floor=floor;this.beds=beds;this.bedType=bedType;bedDimensions=dimensions;this.adults=adults;this.children=children;nightlyRate=rate;}
 public void housekeeping(HousekeepingState state) { housekeeping=java.util.Objects.requireNonNull(state); }
 public void operational(RoomOperationalState state) { operational=java.util.Objects.requireNonNull(state); }
 public void activate(boolean value) { active=value; }
 public boolean canOccupy() { return active && operational==RoomOperationalState.AVAILABLE && (housekeeping==HousekeepingState.CLEAN || housekeeping==HousekeepingState.INSPECTED); }
}

