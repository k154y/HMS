package com.hotelmanagement.hms.room.model;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="room_types")
public class RoomType extends com.hotelmanagement.hms.shared.model.BranchEntity {
    @Column(name="code") private String code;
    @Column(name="name") private String name;
    @Column(name="description",columnDefinition="text") private String description;
    @Column(name="standard_occupancy") private int standardOccupancy;
    @Column(name="max_adults") private int maxAdults;
    @Column(name="max_children") private int maxChildren;
    @Column(name="default_rate",precision=19,scale=4) private BigDecimal defaultRate;
    @Column(name="bed_type") private String bedType="DOUBLE";
    @Column(name="bed_dimensions") private String bedDimensions="160 x 200 cm";
    private boolean active=true;
    protected RoomType() {}
    public static RoomType create(UUID hotelId, UUID branchId, String code, String name, String description, int standardOccupancy, int maxAdults, int maxChildren, BigDecimal defaultRate) {
        var e=new RoomType();
        e.hotelId=hotelId;
        e.branchId=branchId;
        e.code=code;
        e.name=name;
        e.description=description;
        e.standardOccupancy=standardOccupancy;
        e.maxAdults=maxAdults;
        e.maxChildren=maxChildren;
        e.defaultRate=defaultRate;
        return e;
    }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getStandardOccupancy() { return standardOccupancy; }
    public int getMaxAdults() { return maxAdults; }
    public int getMaxChildren() { return maxChildren; }
    public BigDecimal getDefaultRate() { return defaultRate; }
 public String getBedType(){return bedType;} public String getBedDimensions(){return bedDimensions;} public boolean getActive(){return active;} public void archive(){active=false;}
 public void update(String code,String name,String description,int standard,int adults,int children,BigDecimal rate,String bedType,String dimensions){this.code=code;this.name=name;this.description=description;standardOccupancy=standard;maxAdults=adults;maxChildren=children;defaultRate=rate;this.bedType=bedType;bedDimensions=dimensions;}
 public void changeRate(BigDecimal rate) { defaultRate=com.hotelmanagement.hms.shared.model.Money.nonnegative(rate); }
}

