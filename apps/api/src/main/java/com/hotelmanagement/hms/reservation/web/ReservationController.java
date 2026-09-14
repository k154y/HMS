package com.hotelmanagement.hms.reservation.web;
import com.hotelmanagement.hms.reservation.dto.*; import com.hotelmanagement.hms.reservation.service.ReservationService; import com.hotelmanagement.hms.shared.web.PageResponse; import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*; import org.springframework.http.HttpStatus; import java.util.*; import java.time.LocalDate;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/reservations")
public class ReservationController { private final ReservationService service; public ReservationController(ReservationService s){service=s;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ReservationResponse create(@PathVariable UUID hotel,@PathVariable UUID branch,@Valid @RequestBody ReservationRequest r){return service.create(hotel,branch,r);}
 @GetMapping("/availability") public PageResponse<com.hotelmanagement.hms.room.dto.RoomResponse> availability(@PathVariable UUID hotel,@PathVariable UUID branch,@RequestParam LocalDate checkIn,@RequestParam LocalDate checkOut,@RequestParam(defaultValue="1") int adults,@RequestParam(defaultValue="0") int children,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return service.available(hotel,branch,checkIn,checkOut,adults,children,page,size);}
 @GetMapping public PageResponse<ReservationResponse> list(@PathVariable UUID hotel,@PathVariable UUID branch,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="50")int size){return service.list(hotel,branch,page,size);}
 @GetMapping("/{id}") public ReservationResponse get(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id){return service.get(hotel,branch,id);}
 @PostMapping("/{id}/cancel") @ResponseStatus(HttpStatus.NO_CONTENT) public void cancel(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id){service.cancel(hotel,branch,id);}
}
