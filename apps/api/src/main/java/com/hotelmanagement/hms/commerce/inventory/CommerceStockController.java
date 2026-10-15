package com.hotelmanagement.hms.commerce.inventory;
import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/commerce/businesses/{business}")
public class CommerceStockController {
    private final CommerceStockService service;
    private final AuthenticatedUserContext current;
    public CommerceStockController(CommerceStockService service,AuthenticatedUserContext current){this.service=service;this.current=current;}
    @PostMapping("/stock-commands") public Map<String,Object> post(@PathVariable UUID business,@RequestBody CommerceStockService.Command command){return service.post(current.requireCurrentUserId(),business,command);}
    @GetMapping("/locations/{location}/ledger") public List<Map<String,Object>> ledger(@PathVariable UUID business,@PathVariable UUID location,@RequestParam(required=false) UUID product){return service.ledger(current.requireCurrentUserId(),business,location,product);}
}
