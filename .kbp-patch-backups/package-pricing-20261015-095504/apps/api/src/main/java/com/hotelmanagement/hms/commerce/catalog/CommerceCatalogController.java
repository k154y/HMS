package com.hotelmanagement.hms.commerce.catalog;

import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/commerce/businesses/{business}")
public class CommerceCatalogController {
    private final CommerceCatalogService service;
    private final AuthenticatedUserContext current;
    public CommerceCatalogController(CommerceCatalogService service,AuthenticatedUserContext current) {this.service=service;this.current=current;}
    public record CategoryInput(@NotBlank String name,UUID parentId) {}
    public record ProductInput(UUID categoryId,@NotBlank String sku,String barcode,@NotBlank String name,@NotBlank String baseUnit,
        @NotNull BigDecimal purchaseCost,@NotNull BigDecimal retailPrice,BigDecimal wholesalePrice) {}
    public record UnitInput(@NotBlank String name,@NotNull BigDecimal factor) {}
    public record BranchPrice(UUID branchId,BigDecimal price,java.time.OffsetDateTime effectiveAt) {}
    @PostMapping("/products/{product}/update") public Object update(@PathVariable UUID business,@PathVariable UUID product,@RequestBody CommerceCatalogService.ProductUpdate p){service.updateProduct(current.requireCurrentUserId(),business,product,p);return Map.of("updated",true);}
    @PostMapping("/products/{product}/branch-prices") public Object price(@PathVariable UUID business,@PathVariable UUID product,@RequestBody BranchPrice p){service.branchPrice(current.requireCurrentUserId(),business,product,p.branchId(),p.price(),p.effectiveAt());return Map.of("updated",true);}
    @GetMapping("/categories") public List<Map<String,Object>> categories(@PathVariable UUID business) {return service.categories(current.requireCurrentUserId(),business);}
    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> category(@PathVariable UUID business,@Valid @RequestBody CategoryInput body){return service.addCategory(current.requireCurrentUserId(),business,body.name(),body.parentId());}
    @GetMapping("/products") public List<Map<String,Object>> products(@PathVariable UUID business){return service.products(current.requireCurrentUserId(),business);}
    @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> product(@PathVariable UUID business,@Valid @RequestBody ProductInput p){return service.addProduct(current.requireCurrentUserId(),business,p.categoryId(),p.sku(),p.barcode(),p.name(),p.baseUnit(),p.purchaseCost(),p.retailPrice(),p.wholesalePrice());}
    @GetMapping("/products/{product}/units") public List<Map<String,Object>> units(@PathVariable UUID business,@PathVariable UUID product){return service.units(current.requireCurrentUserId(),business,product);}
    @PostMapping("/products/{product}/units") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> unit(@PathVariable UUID business,@PathVariable UUID product,@Valid @RequestBody UnitInput input){return service.addUnit(current.requireCurrentUserId(),business,product,input.name(),input.factor());}
}
