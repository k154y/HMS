package com.hotelmanagement.hms.room.web;
import com.hotelmanagement.hms.room.dto.*;
import com.hotelmanagement.hms.room.model.HousekeepingState;
import com.hotelmanagement.hms.room.service.RoomService;
import com.hotelmanagement.hms.shared.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}")
public class RoomController {
 private final RoomService service;
 public RoomController(RoomService service) { this.service=service; }
 public record RateRequest(@NotNull @DecimalMin("0") @Digits(integer=15,fraction=4) BigDecimal rate) {}
 public record CleaningRequest(@NotNull HousekeepingState state) {}
 @PostMapping("/room-types") @ResponseStatus(HttpStatus.CREATED) public RoomTypeResponse type(@PathVariable UUID hotel,@PathVariable UUID branch,@Valid @RequestBody RoomTypeRequest r) { return service.type(hotel,branch,r); }
 @GetMapping("/room-types") public PageResponse<RoomTypeResponse> types(@PathVariable UUID hotel,@PathVariable UUID branch,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) { return service.types(hotel,branch,page,size); }
 @PutMapping("/room-types/{id}/rate") public RoomTypeResponse rate(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@Valid @RequestBody RateRequest r) { return service.rate(hotel,branch,id,r.rate()); }
 @PostMapping("/rooms") @ResponseStatus(HttpStatus.CREATED) public RoomResponse room(@PathVariable UUID hotel,@PathVariable UUID branch,@Valid @RequestBody RoomRequest r) { return service.create(hotel,branch,r); }
 @GetMapping("/rooms") public PageResponse<RoomResponse> rooms(@PathVariable UUID hotel,@PathVariable UUID branch,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) { return service.rooms(hotel,branch,page,size); }
 @GetMapping("/rooms/{id}") public RoomResponse get(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id) { return service.get(hotel,branch,id); }
 @PutMapping("/rooms/{id}/housekeeping") public RoomResponse cleaning(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@Valid @RequestBody CleaningRequest r) { return service.housekeeping(hotel,branch,id,r.state()); }
}
