package com.hotelmanagement.hms.vendor.web;
import com.hotelmanagement.hms.vendor.dto.*;
import com.hotelmanagement.hms.vendor.service.VendorService;
import com.hotelmanagement.hms.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/vendors")
public class VendorController {
 private final VendorService service;public VendorController(VendorService service){this.service=service;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public VendorResponse create(@PathVariable UUID hotel,@Valid @RequestBody VendorRequest r){return service.create(hotel,r);}
 @GetMapping("/{id}") public VendorResponse get(@PathVariable UUID hotel,@PathVariable UUID id){return service.get(hotel,id);}
 @GetMapping public PageResponse<VendorResponse> list(@PathVariable UUID hotel,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return service.list(hotel,page,size);}
}
