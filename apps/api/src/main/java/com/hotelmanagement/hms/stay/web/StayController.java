package com.hotelmanagement.hms.stay.web;
import com.hotelmanagement.hms.stay.service.StayService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/reservations")
public class StayController {
 private final StayService service;public StayController(StayService s){service=s;}
 @PostMapping("/{id}/check-in") public Object checkIn(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id){return service.checkIn(hotel,branch,id);}
 @PostMapping("/{id}/check-out") public Object checkOut(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id){return service.checkOut(hotel,branch,id);}
}
