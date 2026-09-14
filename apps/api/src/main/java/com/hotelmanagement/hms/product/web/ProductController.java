package com.hotelmanagement.hms.product.web;
import com.hotelmanagement.hms.product.dto.*;
import com.hotelmanagement.hms.product.service.ProductService;
import com.hotelmanagement.hms.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/products")
public class ProductController {
 private final ProductService service;public ProductController(ProductService service){this.service=service;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ProductResponse create(@PathVariable UUID hotel,@Valid @RequestBody ProductRequest r){return service.create(hotel,r);}
 @PutMapping("/{id}") public ProductResponse update(@PathVariable UUID hotel,@PathVariable UUID id,@Valid @RequestBody ProductRequest r){return service.update(hotel,id,r);}
 @GetMapping("/{id}") public ProductResponse get(@PathVariable UUID hotel,@PathVariable UUID id){return service.get(hotel,id);}
 @GetMapping public PageResponse<ProductResponse> list(@PathVariable UUID hotel,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return service.list(hotel,page,size);}
}

