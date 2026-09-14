package com.hotelmanagement.hms.room.dto;
import com.hotelmanagement.hms.room.model.*;import java.util.UUID;import java.math.BigDecimal;
public record RoomResponse(UUID id,UUID roomTypeId,String code,String floor,int beds,String bedType,String bedDimensions,int adults,int children,boolean active,HousekeepingState housekeeping,RoomOperationalState operational,BigDecimal nightlyRate){
 public static RoomResponse from(Room r){return from(r,r.getNightlyRate());}
 public static RoomResponse from(Room r,BigDecimal rate){return new RoomResponse(r.getId(),r.getRoomTypeId(),r.getCode(),r.getFloor(),r.getBeds(),r.getBedType(),r.getBedDimensions(),r.getAdults(),r.getChildren(),r.getActive(),r.getHousekeeping(),r.getOperational(),rate);}
}
