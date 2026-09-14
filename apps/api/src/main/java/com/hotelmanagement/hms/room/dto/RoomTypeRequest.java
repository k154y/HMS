package com.hotelmanagement.hms.room.dto;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record RoomTypeRequest(@NotBlank @Size(max=100) String code,@NotBlank @Size(max=200) String name,@Size(max=2000) String description,
 @Min(1) int standardOccupancy,@Min(1) int maxAdults,@Min(0) int maxChildren,@NotNull @DecimalMin("0") @Digits(integer=15,fraction=4) BigDecimal defaultRate,
 @NotBlank @Size(max=100) String bedType,@NotBlank @Size(max=100) String bedDimensions){
 public RoomTypeRequest(String code,String name,String description,int occupancy,int adults,int children,BigDecimal rate){this(code,name,description,occupancy,adults,children,rate,"DOUBLE","160 x 200 cm");}
}
