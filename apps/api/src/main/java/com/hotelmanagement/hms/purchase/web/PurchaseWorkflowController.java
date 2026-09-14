package com.hotelmanagement.hms.purchase.web;
import com.hotelmanagement.hms.purchase.service.PurchaseOrderService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/purchase-orders")
public class PurchaseWorkflowController {
 private final PurchaseOrderService service;public PurchaseWorkflowController(PurchaseOrderService s){service=s;}
 @GetMapping public List<Map<String,Object>> list(@PathVariable UUID hotel,@PathVariable UUID branch){return service.list(hotel,branch);}
 @GetMapping("/{id}") public Map<String,Object> detail(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id){return service.detail(hotel,branch,id);}
 @PostMapping("/{id}/payments") public Map<String,Object> pay(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@RequestBody PurchaseOrderService.VendorPayment request){return service.pay(hotel,branch,id,request);}
}
