package com.hotelmanagement.hms.customer.web;
import com.hotelmanagement.hms.customer.dto.*;
import com.hotelmanagement.hms.customer.service.CustomerService;
import com.hotelmanagement.hms.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/customers")
public class CustomerController {
 private final CustomerService service;
 public CustomerController(CustomerService service) { this.service=service; }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public CustomerResponse create(@PathVariable UUID hotel,@Valid @RequestBody CustomerRequest r) { return service.create(hotel,r); }
 @PutMapping("/{id}") public CustomerResponse update(@PathVariable UUID hotel,@PathVariable UUID id,@Valid @RequestBody CustomerRequest r) { return service.update(hotel,id,r); }
 @GetMapping("/{id}") public CustomerResponse get(@PathVariable UUID hotel,@PathVariable UUID id) { return service.get(hotel,id); }
 @GetMapping public PageResponse<CustomerResponse> list(@PathVariable UUID hotel,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) { return service.list(hotel,page,size); }
 @PostMapping("/guests") @ResponseStatus(HttpStatus.CREATED) public GuestResponse guest(@PathVariable UUID hotel,@Valid @RequestBody GuestRequest r) { return service.guest(hotel,r); }
 @GetMapping("/guests") public PageResponse<GuestResponse> guests(@PathVariable UUID hotel,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) { return service.guests(hotel,page,size); }
}
