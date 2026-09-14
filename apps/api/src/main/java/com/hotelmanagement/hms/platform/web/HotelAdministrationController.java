package com.hotelmanagement.hms.platform.web;
import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import com.hotelmanagement.hms.platform.service.HotelAdministrationService;
import com.hotelmanagement.hms.platform.dto.*;
import com.hotelmanagement.hms.platform.currency.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.hotelmanagement.hms.shared.web.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hotels/{hotelId}")
public class HotelAdministrationController {
    private final HotelAdministrationService service;
    private final AuthenticatedUserContext user;
    public HotelAdministrationController(HotelAdministrationService service, AuthenticatedUserContext user) {
        this.service=service; this.user=user;
    }
    public record BranchStatusRequest(@NotNull Boolean active) {}
    @GetMapping public HotelResponse get(@PathVariable UUID hotelId) { return service.get(user.requireCurrentUserId(),hotelId); }
    @PutMapping public HotelResponse update(@PathVariable UUID hotelId,@Valid @RequestBody CreateHotelRequest request) {
        return service.update(user.requireCurrentUserId(),hotelId,request);
    }
    @GetMapping("/branches")
    public PageResponse<BranchResponse> list(@PathVariable UUID hotelId,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="50") int size) { return PageResponse.from(service.branches(user.requireCurrentUserId(),hotelId,page,size)); }
    @GetMapping("/branches/{branchId}")
    public BranchResponse branch(@PathVariable UUID hotelId,@PathVariable UUID branchId) {
        return service.branch(user.requireCurrentUserId(),hotelId,branchId);
    }
    @PostMapping("/branches") @ResponseStatus(HttpStatus.CREATED)
    public BranchResponse createBranch(@PathVariable UUID hotelId,@Valid @RequestBody CreateBranchRequest request) {
        return service.createBranch(user.requireCurrentUserId(),hotelId,request);
    }
    @PutMapping("/branches/{branchId}")
    public BranchResponse updateBranch(@PathVariable UUID hotelId,@PathVariable UUID branchId,@Valid @RequestBody CreateBranchRequest request) {
        return service.updateBranch(user.requireCurrentUserId(),hotelId,branchId,request);
    }
    @PutMapping("/branches/{branchId}/status")
    public BranchResponse branchStatus(@PathVariable UUID hotelId,@PathVariable UUID branchId,@Valid @RequestBody BranchStatusRequest request) {
        return service.branchStatus(user.requireCurrentUserId(),hotelId,branchId,request.active());
    }
    @PostMapping("/exchange-rates") @ResponseStatus(HttpStatus.CREATED)
    public ExchangeRateResponse createRate(@PathVariable UUID hotelId,@Valid @RequestBody ExchangeRateRequest request) {
        return service.createRate(user.requireCurrentUserId(),hotelId,request);
    }
    @GetMapping("/exchange-rates")
    public PageResponse<ExchangeRateResponse> history(@PathVariable UUID hotelId,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="50") int size) { return PageResponse.from(service.history(user.requireCurrentUserId(),hotelId,page,size)); }
    @GetMapping("/exchange-rates/applicable")
    public ExchangeRateResponse applicable(@PathVariable UUID hotelId,@RequestParam String currency,
            @RequestParam(required=false) OffsetDateTime effectiveAt) {
        return service.rate(user.requireCurrentUserId(),hotelId,currency,effectiveAt);
    }
}
