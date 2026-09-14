package com.hotelmanagement.hms.room.dto;
import com.hotelmanagement.hms.room.model.RoomType;import java.util.UUID;import java.math.BigDecimal;
public record RoomTypeResponse(UUID id,String code,String name,String description,int standardOccupancy,int maxAdults,int maxChildren,BigDecimal defaultRate,String bedType,String bedDimensions,boolean active){
 public static RoomTypeResponse from(RoomType r){return new RoomTypeResponse(r.getId(),r.getCode(),r.getName(),r.getDescription(),r.getStandardOccupancy(),r.getMaxAdults(),r.getMaxChildren(),r.getDefaultRate(),r.getBedType(),r.getBedDimensions(),r.getActive());}
}
