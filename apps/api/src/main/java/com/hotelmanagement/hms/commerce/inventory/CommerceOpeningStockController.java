package com.hotelmanagement.hms.commerce.inventory;

import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/commerce/businesses/{business}/locations/{location}")
public class CommerceOpeningStockController {
    private final CommerceOpeningStockService service;
    private final AuthenticatedUserContext current;
    public CommerceOpeningStockController(CommerceOpeningStockService service,AuthenticatedUserContext current){this.service=service;this.current=current;}
    public record OpeningStockInput(@NotNull UUID productId,@NotNull UUID requestId,@NotNull BigDecimal quantity) {}
    @GetMapping("/stock")
    public List<Map<String,Object>> balances(@PathVariable UUID business,@PathVariable UUID location){return service.balances(current.requireCurrentUserId(),business,location);}
    @PostMapping("/opening-stock") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> opening(@PathVariable UUID business,@PathVariable UUID location,@Valid @RequestBody OpeningStockInput input){return service.opening(current.requireCurrentUserId(),business,location,input.productId(),input.requestId(),input.quantity());}
}
