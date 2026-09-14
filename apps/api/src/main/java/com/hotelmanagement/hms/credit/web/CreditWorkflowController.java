package com.hotelmanagement.hms.credit.web;
import com.hotelmanagement.hms.credit.service.CreditService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/credit")
public class CreditWorkflowController {
 private final CreditService service;public CreditWorkflowController(CreditService s){service=s;}
 @GetMapping public Object list(@PathVariable UUID hotel,@PathVariable UUID branch){return service.list(hotel,branch);}
 @GetMapping("/{customer}/ledger") public Object ledger(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID customer){return service.history(hotel,branch,customer);}
 @PostMapping("/folios/{id}/approve") public void approve(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id){service.approveFolio(hotel,branch,id);}
}
