package com.hotelmanagement.hms.identity.administration.web;

import com.hotelmanagement.hms.identity.administration.dto.*;
import com.hotelmanagement.hms.identity.administration.service.StaffAdministrationService;
import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import com.hotelmanagement.hms.identity.authorization.model.PermissionCode;
import com.hotelmanagement.hms.identity.membership.dto.HotelMembershipResponse;
import com.hotelmanagement.hms.identity.membership.model.HotelMembershipStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.hotelmanagement.hms.shared.web.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/hotels/{hotelId}")
public class StaffAdministrationController {
    private final StaffAdministrationService service;
    private final AuthenticatedUserContext user;
    public StaffAdministrationController(StaffAdministrationService service, AuthenticatedUserContext user) {
        this.service=service; this.user=user;
    }
    public record StatusRequest(@NotNull HotelMembershipStatus status) {}
    public record BranchAccessRequest(boolean allBranches, @NotNull @Size(max=100) Set<@NotNull UUID> branchIds) {}
    @GetMapping("/memberships")
    public PageResponse<HotelMembershipResponse> list(@PathVariable UUID hotelId,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) {
        return PageResponse.from(service.listStaff(user.requireCurrentUserId(),hotelId,page,size));
    }
    @PostMapping("/memberships") @ResponseStatus(HttpStatus.CREATED)
    public HotelMembershipResponse add(@PathVariable UUID hotelId,@Valid @RequestBody StaffRequest request) {
        return service.addStaff(user.requireCurrentUserId(),hotelId,request);
    }
    @PutMapping("/memberships/{membershipId}/status")
    public HotelMembershipResponse status(@PathVariable UUID hotelId,@PathVariable UUID membershipId,
            @Valid @RequestBody StatusRequest request) {
        return service.status(user.requireCurrentUserId(),hotelId,membershipId,request.status());
    }
    @PutMapping("/memberships/{membershipId}/branch-access") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void branches(@PathVariable UUID hotelId,@PathVariable UUID membershipId,@Valid @RequestBody BranchAccessRequest request) {
        service.branches(user.requireCurrentUserId(),hotelId,membershipId,request.allBranches(),request.branchIds());
    }
    @GetMapping("/roles")
    public PageResponse<RoleResponse> roles(@PathVariable UUID hotelId,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="50") int size) { return PageResponse.from(service.roles(user.requireCurrentUserId(),hotelId,page,size)); }
    @PostMapping("/roles") @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse create(@PathVariable UUID hotelId,@Valid @RequestBody RoleRequest request) {
        return service.createRole(user.requireCurrentUserId(),hotelId,request);
    }
    @PutMapping("/roles/{roleId}")
    public RoleResponse update(@PathVariable UUID hotelId,@PathVariable UUID roleId,@Valid @RequestBody RoleRequest request) {
        return service.updateRole(user.requireCurrentUserId(),hotelId,roleId,request);
    }
    @GetMapping("/roles/{roleId}/permissions")
    public List<String> permissions(@PathVariable UUID hotelId,@PathVariable UUID roleId) {
        return service.permissions(user.requireCurrentUserId(),hotelId,roleId);
    }
    @PutMapping("/roles/{roleId}/permissions/{permission}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grant(@PathVariable UUID hotelId,@PathVariable UUID roleId,@PathVariable PermissionCode permission) {
        service.permission(user.requireCurrentUserId(),hotelId,roleId,permission,true);
    }
    @DeleteMapping("/roles/{roleId}/permissions/{permission}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID hotelId,@PathVariable UUID roleId,@PathVariable PermissionCode permission) {
        service.permission(user.requireCurrentUserId(),hotelId,roleId,permission,false);
    }
    @PutMapping("/memberships/{membershipId}/roles/{roleId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assign(@PathVariable UUID hotelId,@PathVariable UUID membershipId,@PathVariable UUID roleId) {
        service.assignment(user.requireCurrentUserId(),hotelId,membershipId,roleId,true);
    }
    @DeleteMapping("/memberships/{membershipId}/roles/{roleId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable UUID hotelId,@PathVariable UUID membershipId,@PathVariable UUID roleId) {
        service.assignment(user.requireCurrentUserId(),hotelId,membershipId,roleId,false);
    }
}
