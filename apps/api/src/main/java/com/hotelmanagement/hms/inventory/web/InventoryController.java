package com.hotelmanagement.hms.inventory.web;
import com.hotelmanagement.hms.inventory.dto.*;
import com.hotelmanagement.hms.inventory.model.MovementKind;
import com.hotelmanagement.hms.inventory.service.InventoryService;
import com.hotelmanagement.hms.shared.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/inventory")
public class InventoryController {
 private final InventoryService service;public InventoryController(InventoryService service){this.service=service;}
 public record Adjustment(@NotNull UUID productId,@NotNull @Digits(integer=15,fraction=4) BigDecimal quantity,@NotNull MovementKind kind,@NotNull UUID requestId,@NotBlank @Size(max=1000) String reason){}
 public record Transfer(@NotNull UUID destinationBranchId,@NotNull UUID productId,@NotNull @DecimalMin("0.0001") @Digits(integer=15,fraction=4) BigDecimal quantity,@NotNull UUID requestId,@NotBlank @Size(max=1000) String reason){}
 public record Reason(@NotBlank @Size(max=1000) String reason){}
 @GetMapping("/{product}") public InventoryService.StockResponse stock(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID product){return service.stock(hotel,branch,product);}
 @GetMapping("/{product}/movements") public PageResponse<MovementResponse> history(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID product,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return service.history(hotel,branch,product,page,size);}
 @PostMapping("/adjustments") @ResponseStatus(HttpStatus.CREATED) public MovementResponse adjust(@PathVariable UUID hotel,@PathVariable UUID branch,@Valid @RequestBody Adjustment r){return service.adjust(hotel,branch,r.productId(),r.quantity(),r.kind(),r.requestId(),r.reason());}
 @PostMapping("/transfers") @ResponseStatus(HttpStatus.NO_CONTENT) public void transfer(@PathVariable UUID hotel,@PathVariable UUID branch,@Valid @RequestBody Transfer r){service.transfer(hotel,branch,r.destinationBranchId(),r.productId(),r.quantity(),r.requestId(),r.reason());}
 @PostMapping("/movements/{id}/reversal") @ResponseStatus(HttpStatus.CREATED) public MovementResponse reverse(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@Valid @RequestBody Reason r){return service.reverse(hotel,branch,id,r.reason());}
}
