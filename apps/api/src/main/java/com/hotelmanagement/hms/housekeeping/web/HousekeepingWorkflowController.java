package com.hotelmanagement.hms.housekeeping.web;
import com.hotelmanagement.hms.housekeeping.service.HousekeepingTaskService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/housekeeping/tasks")
public class HousekeepingWorkflowController {
 private final HousekeepingTaskService service;public HousekeepingWorkflowController(HousekeepingTaskService s){service=s;}
 @GetMapping public Object list(@PathVariable UUID hotel,@PathVariable UUID branch){return service.list(hotel,branch);}
 @GetMapping("/staff") public Object staff(@PathVariable UUID hotel,@PathVariable UUID branch){return service.staff(hotel,branch);}
 public record Assignment(UUID userId){}
 @PutMapping("/{id}/assignment") public void assign(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@RequestBody Assignment request){service.assign(hotel,branch,id,request.userId());}
}
