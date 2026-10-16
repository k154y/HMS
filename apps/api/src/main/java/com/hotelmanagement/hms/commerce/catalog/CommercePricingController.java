package com.hotelmanagement.hms.commerce.catalog;
import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;
@RestController
@RequestMapping("/api/v1/commerce/businesses/{business}/pricing")
public class CommercePricingController {
    private final CommercePricingService service;private final AuthenticatedUserContext current;
    public CommercePricingController(CommercePricingService service,AuthenticatedUserContext current){this.service=service;this.current=current;}
    public record Preview(@NotBlank String currency){}
    public record Change(@NotBlank String currency,@NotBlank String previousCurrency,@NotNull BigDecimal rateToBase){}
    public record Save(@NotBlank String currency,@NotNull BigDecimal rateToBase,@NotNull @Valid CommerceCatalogController.NewProduct input){}
    @GetMapping public Object catalog(@PathVariable UUID business){return service.catalog(current.requireCurrentUserId(),business);}
    @PostMapping("/preview") public Object preview(@PathVariable UUID business,@Valid @RequestBody Preview p){return service.preview(current.requireCurrentUserId(),business,p.currency());}
    @PostMapping("/default") public Object choose(@PathVariable UUID business,@Valid @RequestBody Change p){return service.choose(current.requireCurrentUserId(),business,p.currency(),p.previousCurrency(),p.rateToBase());}
    @GetMapping("/products/{product}/units") public Object units(@PathVariable UUID business,@PathVariable UUID product){return service.units(current.requireCurrentUserId(),business,product);}
    @PostMapping("/products") public Object create(@PathVariable UUID business,@Valid @RequestBody Save p){return service.save(current.requireCurrentUserId(),business,null,p.currency(),p.rateToBase(),p.input());}
    @PostMapping("/products/{product}") public Object save(@PathVariable UUID business,@PathVariable UUID product,@Valid @RequestBody Save p){return service.save(current.requireCurrentUserId(),business,product,p.currency(),p.rateToBase(),p.input());}
}
