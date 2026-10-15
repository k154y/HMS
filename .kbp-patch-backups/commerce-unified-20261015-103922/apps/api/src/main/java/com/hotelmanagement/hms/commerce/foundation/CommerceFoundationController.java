package com.hotelmanagement.hms.commerce.foundation;

import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/commerce")
public class CommerceFoundationController {
    private final CommerceFoundationService service;
    private final AuthenticatedUserContext current;
    public CommerceFoundationController(CommerceFoundationService service, AuthenticatedUserContext current) {
        this.service=service;this.current=current;
    }
    public record BusinessInput(@NotBlank String name,@NotBlank String baseCurrency) {}
    public record BranchInput(@NotBlank String name) {}
    public record LocationInput(@NotBlank String name,@NotBlank String type) {}
    @PostMapping("/businesses") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> create(@Valid @RequestBody BusinessInput input){return service.create(current.requireCurrentUserId(),input.name(),input.baseCurrency());}
    @GetMapping("/businesses")
    public List<Map<String,Object>> businesses(){return service.myBusinesses(current.requireCurrentUserId());}
    @GetMapping("/businesses/{business}/branches")
    public List<Map<String,Object>> branches(@PathVariable UUID business){return service.branches(current.requireCurrentUserId(),business);}
    @PostMapping("/businesses/{business}/branches") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> addBranch(@PathVariable UUID business,@Valid @RequestBody BranchInput input){return service.addBranch(current.requireCurrentUserId(),business,input.name());}
    @GetMapping("/businesses/{business}/branches/{branch}/locations")
    public List<Map<String,Object>> locations(@PathVariable UUID business,@PathVariable UUID branch){return service.locations(current.requireCurrentUserId(),business,branch);}
    @PostMapping("/businesses/{business}/branches/{branch}/locations") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> addLocation(@PathVariable UUID business,@PathVariable UUID branch,@Valid @RequestBody LocationInput input){return service.addLocation(current.requireCurrentUserId(),business,branch,input.name(),input.type());}
}
