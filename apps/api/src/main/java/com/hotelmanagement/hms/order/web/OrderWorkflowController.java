package com.hotelmanagement.hms.order.web;
import com.hotelmanagement.hms.order.service.OrderService;
import com.hotelmanagement.hms.order.model.OrderStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/orders")
public class OrderWorkflowController {
 private final OrderService service;public OrderWorkflowController(OrderService service){this.service=service;}
 @GetMapping public Object list(@PathVariable UUID hotel,@PathVariable UUID branch){return service.list(hotel,branch);}
 @GetMapping("/{id}") public Object detail(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id){return service.detail(hotel,branch,id);}
 public record StateRequest(OrderStatus status){}
 @GetMapping("/queue") public Object queue(@PathVariable UUID hotel,@PathVariable UUID branch,@RequestParam String destination){return service.queue(hotel,branch,destination);}
 @PostMapping("/{id}/items/{item}/status") public void prepare(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@PathVariable UUID item,@RequestBody StateRequest body){service.prepare(hotel,branch,id,item,body.status());}
 @PostMapping("/{id}/status") public Object advance(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@RequestBody StateRequest body){return service.advance(hotel,branch,id,body.status());}
}
