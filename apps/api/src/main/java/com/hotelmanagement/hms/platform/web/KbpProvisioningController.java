package com.hotelmanagement.hms.platform.web;
import com.hotelmanagement.hms.platform.service.KbpProvisioningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/platform/organizations")
public class KbpProvisioningController {
    private final KbpProvisioningService service;
    public KbpProvisioningController(KbpProvisioningService service){this.service=service;}
    @GetMapping public List<Map<String,Object>> list(){return service.organizations();}
    public record Modules(UUID business,Set<String> modules) {}
    @GetMapping("/modules") public Object modules(@RequestParam UUID business){return service.commerceModules(business);}
    @PostMapping("/modules") public Object modules(@RequestBody Modules p){return service.commerceModules(p.business(),p.modules());}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> create(@Valid @RequestBody KbpProvisioningService.CreateOrganization input){
        return service.create(input);
    }
}
