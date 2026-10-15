package com.hotelmanagement.hms.commerce.foundation;

import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/commerce/businesses/{business}")
public class CommerceConfigurationController {
    private final CommerceConfigurationService service;
    private final AuthenticatedUserContext current;
    public CommerceConfigurationController(CommerceConfigurationService service,AuthenticatedUserContext current){this.service=service;this.current=current;}
    public record Category(@NotBlank String name,UUID parentId,@NotNull Boolean active) {}
    public record Branch(@NotBlank String name,String contact,@NotNull Boolean active) {}
    public record Location(@NotBlank String name,@NotNull Boolean active) {}
    public record CurrencyInput(@NotBlank String currency,@NotNull Boolean active) {}
    public record Rate(@NotBlank String currency,@NotNull BigDecimal rate,@NotNull OffsetDateTime effectiveAt,@NotBlank String source) {}
    public record Quote(@NotBlank String currency,@NotNull BigDecimal amount,@NotNull OffsetDateTime at) {}
    public record Employee(@NotBlank @Email String email,@NotBlank String name,@NotBlank String temporaryPassword,@NotBlank String role,@NotEmpty List<UUID> branches) {
        @Override public String toString(){return "Employee[REDACTED]";}
    }
    public record EmployeeUpdate(@NotBlank String role,@NotNull Boolean active,@NotEmpty List<UUID> branches) {}
    @PostMapping("/categories/{id}/update") public Map<String,Boolean> category(@PathVariable UUID business,@PathVariable UUID id,@Valid @RequestBody Category p){service.category(current.requireCurrentUserId(),business,id,p.name(),p.parentId(),p.active());return Map.of("updated",true);}
    @PostMapping("/branches/{id}/update") public Map<String,Boolean> branch(@PathVariable UUID business,@PathVariable UUID id,@Valid @RequestBody Branch p){service.branch(current.requireCurrentUserId(),business,id,p.name(),p.contact(),p.active());return Map.of("updated",true);}
    @PostMapping("/locations/{id}/update") public Map<String,Boolean> location(@PathVariable UUID business,@PathVariable UUID id,@Valid @RequestBody Location p){service.location(current.requireCurrentUserId(),business,id,p.name(),p.active());return Map.of("updated",true);}
    @GetMapping("/currencies") public List<Map<String,Object>> currencies(@PathVariable UUID business){return service.currencies(current.requireCurrentUserId(),business);}
    @PostMapping("/currencies") public Map<String,Boolean> currency(@PathVariable UUID business,@Valid @RequestBody CurrencyInput p){service.configureCurrency(current.requireCurrentUserId(),business,p.currency(),p.active());return Map.of("updated",true);}
    @GetMapping("/exchange-rates") public List<Map<String,Object>> rates(@PathVariable UUID business){return service.rates(current.requireCurrentUserId(),business);}
    @PostMapping("/exchange-rates") public Map<String,UUID> rate(@PathVariable UUID business,@Valid @RequestBody Rate p){return Map.of("id",service.rate(current.requireCurrentUserId(),business,p.currency(),p.rate(),p.effectiveAt(),p.source()));}
    @PostMapping("/currency-quote") public CommerceConfigurationService.Quote quote(@PathVariable UUID business,@Valid @RequestBody Quote p){return service.quote(current.requireCurrentUserId(),business,p.currency(),p.amount(),p.at());}
    @GetMapping("/employees") public List<Map<String,Object>> staff(@PathVariable UUID business){return service.staff(current.requireCurrentUserId(),business);}
    @PostMapping("/employees") public Map<String,UUID> employee(@PathVariable UUID business,@Valid @RequestBody Employee p){return Map.of("id",service.createStaff(current.requireCurrentUserId(),business,p.email(),p.name(),p.temporaryPassword(),p.role(),p.branches()));}
    @PostMapping("/employees/{id}/update") public Map<String,Boolean> employee(@PathVariable UUID business,@PathVariable UUID id,@Valid @RequestBody EmployeeUpdate p){service.updateStaff(current.requireCurrentUserId(),business,id,p.role(),p.active(),p.branches());return Map.of("updated",true);}
    @GetMapping("/configuration-audit") public List<Map<String,Object>> history(@PathVariable UUID business){return service.history(current.requireCurrentUserId(),business);}
}
