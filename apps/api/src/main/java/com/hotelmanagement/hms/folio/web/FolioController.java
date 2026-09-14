package com.hotelmanagement.hms.folio.web;
import com.hotelmanagement.hms.folio.dto.*;
import com.hotelmanagement.hms.folio.service.FolioService;
import com.hotelmanagement.hms.shared.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import java.math.BigDecimal;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/folios")
public class FolioController {
 private final FolioService service;public FolioController(FolioService service){this.service=service;}
 @GetMapping public Object list(@PathVariable UUID hotel,@PathVariable UUID branch){return service.list(hotel,branch);}
 public record Open(@NotNull UUID customerId){}
 public record Charge(@NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=15,fraction=4) BigDecimal amount,@NotBlank @Size(max=1000) String memo,@NotNull UUID requestId){}
 public record Reason(@NotBlank @Size(max=1000) String reason){}
 public record Transfer(@NotNull UUID destinationId,@NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=15,fraction=4) BigDecimal amount,@NotBlank @Size(max=1000) String reason,@NotNull UUID requestId){}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public FolioResponse open(@PathVariable UUID hotel,@PathVariable UUID branch,@Valid @RequestBody Open r){return service.create(hotel,branch,r.customerId());}
 @GetMapping("/{id}") public FolioResponse get(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id){return service.get(hotel,branch,id);}
 @GetMapping("/{id}/entries") public PageResponse<FolioEntryResponse> entries(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return service.entries(hotel,branch,id,page,size);}
 @PostMapping("/{id}/charges") @ResponseStatus(HttpStatus.CREATED) public FolioEntryResponse charge(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@Valid @RequestBody Charge r){return service.charge(hotel,branch,id,r.amount(),r.memo(),r.requestId());}
 @PostMapping("/{id}/entries/{entry}/reversal") @ResponseStatus(HttpStatus.CREATED) public FolioEntryResponse reverse(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@PathVariable UUID entry,@Valid @RequestBody Reason r){return service.reverse(hotel,branch,id,entry,r.reason());}
 @PostMapping("/{id}/transfers") @ResponseStatus(HttpStatus.NO_CONTENT) public void transfer(@PathVariable UUID hotel,@PathVariable UUID branch,@PathVariable UUID id,@Valid @RequestBody Transfer r){service.transfer(hotel,branch,id,r.destinationId(),r.amount(),r.reason(),r.requestId());}
}
