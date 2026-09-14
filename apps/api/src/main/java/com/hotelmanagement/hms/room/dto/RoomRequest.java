package com.hotelmanagement.hms.room.dto;
import jakarta.validation.constraints.*;import java.util.UUID;import java.math.BigDecimal;
public record RoomRequest(@NotNull UUID roomTypeId,@NotBlank @Size(max=100) String code,@Size(max=50) String floor,
 @Min(1) int beds,@Size(max=100) String bedType,@Size(max=100) String bedDimensions,@Min(0) Integer adults,@Min(0) Integer children,
 @DecimalMin("0") @Digits(integer=15,fraction=4) BigDecimal nightlyRate){
 public RoomRequest(UUID type,String code,String floor,int beds,String bedType,String dimensions,int adults,int children){this(type,code,floor,beds,bedType,dimensions,adults,children,null);}
}
