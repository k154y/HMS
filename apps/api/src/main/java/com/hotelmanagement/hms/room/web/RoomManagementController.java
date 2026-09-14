package com.hotelmanagement.hms.room.web;
import com.hotelmanagement.hms.room.service.RoomService;import com.hotelmanagement.hms.room.dto.*;
import org.springframework.web.bind.annotation.*;import org.springframework.http.HttpStatus;import jakarta.validation.Valid;import java.util.UUID;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}")
public class RoomManagementController {
 private final RoomService service;public RoomManagementController(RoomService s){service=s;}
 @PutMapping("/rooms/{id}") public RoomResponse edit(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@Valid @RequestBody RoomRequest r){return service.edit(hotel,branch,id,r);}
 @DeleteMapping("/rooms/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id){service.delete(hotel,branch,id);}
 @PutMapping("/room-types/{id}") public RoomTypeResponse editType(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@Valid @RequestBody RoomTypeRequest r){return service.editType(hotel,branch,id,r);}
 @DeleteMapping("/room-types/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteType(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id){service.deleteType(hotel,branch,id);}
}
