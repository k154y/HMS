package com.hotelmanagement.hms.payment.web;
import com.hotelmanagement.hms.payment.service.PaymentWorkflow;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/payment-approvals")
public class PaymentApprovalController {
 private final PaymentWorkflow service;public PaymentApprovalController(PaymentWorkflow s){service=s;}
 public record Decision(String reason){}
 @GetMapping public Object list(@PathVariable UUID hotel,@PathVariable UUID branch){return service.list(hotel,branch);}
 @PostMapping public Object request(@PathVariable UUID hotel,@PathVariable UUID branch,@RequestBody PaymentWorkflow.Request r){return service.request(hotel,branch,r);}
 @PostMapping("/{id}/approve") public Object approve(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@RequestBody(required=false) Decision decision){return service.approve(hotel,branch,id,true,decision==null?null:decision.reason());}
 @PostMapping("/{id}/reject") public Object reject(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@RequestBody(required=false) Decision decision){return service.approve(hotel,branch,id,false,decision==null?null:decision.reason());}
}

