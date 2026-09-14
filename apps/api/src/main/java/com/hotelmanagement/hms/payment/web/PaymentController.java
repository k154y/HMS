package com.hotelmanagement.hms.payment.web;
import com.hotelmanagement.hms.payment.dto.*;
import com.hotelmanagement.hms.payment.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/payments")
public class PaymentController {
 private final PaymentService service; private final PaymentWorkflow workflow;
 public PaymentController(PaymentService s,PaymentWorkflow w){service=s;workflow=w;}
 @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
 public Map<String,Object> create(@PathVariable UUID hotel,@PathVariable UUID branch,@Valid @RequestBody PaymentRequest r){return workflow.requestSingle(hotel,branch,r);}
 @GetMapping("/{id}") public PaymentResponse get(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id){return service.get(hotel,branch,id);}
 @PostMapping("/{id}/refund") @ResponseStatus(HttpStatus.NO_CONTENT) public void refund(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id){service.refund(hotel,branch,id);}
 @PostMapping("/{id}/void") @ResponseStatus(HttpStatus.NO_CONTENT) public void voidPayment(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id){service.voidPayment(hotel,branch,id);}
}
