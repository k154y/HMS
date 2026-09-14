package com.hotelmanagement.hms.identity.administration.service;

import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.identity.administration.dto.*;
import com.hotelmanagement.hms.identity.authorization.model.*;
import com.hotelmanagement.hms.identity.authorization.repository.*;
import com.hotelmanagement.hms.identity.authorization.service.*;
import com.hotelmanagement.hms.identity.membership.dto.HotelMembershipResponse;
import com.hotelmanagement.hms.identity.membership.model.HotelMembershipStatus;
import com.hotelmanagement.hms.identity.membership.repository.HotelMembershipRepository;
import com.hotelmanagement.hms.identity.membership.service.HotelMembershipService;
import com.hotelmanagement.hms.identity.service.UserAccountService;
import com.hotelmanagement.hms.platform.repository.HotelRepository;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Transactional
public class StaffAdministrationService {
    private final AuthorizationGuard guard;
    private final HotelMembershipService memberships;
    private final HotelMembershipRepository membershipRepository;
    private final UserAccountService accounts;
    private final RoleRepository roles;
    private final RoleAuthorizationService assignments;
    private final RolePermissionRepository permissions;
    private final HotelRepository hotels;
    private final AuditService audit;
    public StaffAdministrationService(AuthorizationGuard guard, HotelMembershipService memberships,
            HotelMembershipRepository membershipRepository, UserAccountService accounts, RoleRepository roles,
            RoleAuthorizationService assignments, RolePermissionRepository permissions, HotelRepository hotels, AuditService audit) {
        this.guard = guard; this.memberships = memberships; this.membershipRepository = membershipRepository;
        this.accounts = accounts; this.roles = roles; this.assignments = assignments; this.permissions = permissions;
        this.hotels = hotels; this.audit = audit;
    }
    @Transactional(readOnly=true)
    public Page<HotelMembershipResponse> listStaff(UUID actor, UUID hotel, int page, int size) {
        guard.requireHotelPermission(actor, hotel, PermissionCode.USER_VIEW);
        return membershipRepository.findByHotel_Id(hotel, page(page,size)).map(HotelMembershipResponse::from);
    }
    public HotelMembershipResponse addStaff(UUID actor, UUID hotel, StaffRequest request) {
        guard.requireHotelPermission(actor, hotel, PermissionCode.USER_MANAGE);
        UUID identity = request.existingUserId();
        if (identity != null) {
            if (request.email()!=null || request.password()!=null || request.fullName()!=null || request.phone()!=null || request.preferredLanguage()!=null)
                throw new IllegalArgumentException("Existing identities cannot be overwritten.");
        } else {
            identity = accounts.createAccount(request.email(), request.password(), request.fullName(),
                    request.phone(), request.preferredLanguage()).getId();
        }
        var result = memberships.createMembership(identity, hotel, request.allBranches());
        audit.record(hotel,null,actor,"MEMBERSHIP_CREATED","MEMBERSHIP",result.id());
        return result;
    }
    public HotelMembershipResponse status(UUID actor, UUID hotel, UUID membership, HotelMembershipStatus status) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.USER_MANAGE);
        requireMembership(hotel,membership);
        var result = switch(status) {
            case ACTIVE -> memberships.activateMembership(hotel,membership);
            case SUSPENDED -> memberships.suspendMembership(hotel,membership);
            case REVOKED -> memberships.revokeMembership(hotel,membership);
        };
        audit.record(hotel,null,actor,"MEMBERSHIP_"+status,"MEMBERSHIP",membership);
        return result;
    }
    public void branches(UUID actor, UUID hotel, UUID membership, boolean all, Set<UUID> branchIds) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.USER_MANAGE);
        requireMembership(hotel,membership);
        if (branchIds==null || branchIds.size()>100 || (all && !branchIds.isEmpty()))
            throw new IllegalArgumentException("Invalid branch selection.");
        // Clear and replace mapping rows atomically; historical audit events remain.
        memberships.grantAllBranches(hotel,membership);
        if (!all) {
            memberships.restrictToSelectedBranches(hotel,membership);
            for (UUID branch : branchIds) memberships.grantBranchAccess(hotel,membership,branch);
        }
        audit.record(hotel,null,actor,"BRANCH_ACCESS_REPLACED","MEMBERSHIP",membership);
    }
    @Transactional(readOnly=true)
    public Page<RoleResponse> roles(UUID actor, UUID hotel, int page, int size) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.ROLE_VIEW);
        return roles.findByHotel_Id(hotel,page(page,size)).map(RoleResponse::from);
    }
    public RoleResponse createRole(UUID actor, UUID hotel, RoleRequest request) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.ROLE_MANAGE);
        var role = Role.create(hotels.findById(hotel).orElseThrow(ApiException::notFound),
                request.code(),request.name().trim(),request.description(),false,now());
        if (!request.active()) role.deactivate(now());
        roles.saveAndFlush(role);
        audit.record(hotel,null,actor,"ROLE_CREATED","ROLE",role.getId());
        return RoleResponse.from(role);
    }
    public RoleResponse updateRole(UUID actor, UUID hotel, UUID id, RoleRequest request) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.ROLE_MANAGE);
        var role = requireCustomRole(hotel,id);
        if (!role.getCode().equals(request.code())) throw new IllegalArgumentException("Role code is immutable.");
        role.rename(request.name().trim(),request.description(),now());
        if(request.active()) role.activate(now()); else role.deactivate(now());
        audit.record(hotel,null,actor,"ROLE_UPDATED","ROLE",id);
        return RoleResponse.from(role);
    }
    public void permission(UUID actor, UUID hotel, UUID role, PermissionCode code, boolean grant) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.ROLE_MANAGE);
        requireCustomRole(hotel,role);
        if(grant) assignments.assignPermissionToRole(hotel,role,code.name());
        else assignments.removePermissionFromRole(hotel,role,code.name());
        audit.record(hotel,null,actor,(grant?"PERMISSION_GRANTED_":"PERMISSION_REMOVED_")+code,"ROLE",role);
    }
    @Transactional(readOnly=true)
    public List<String> permissions(UUID actor, UUID hotel, UUID role) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.ROLE_VIEW);
        roles.findByIdAndHotel_Id(role,hotel).orElseThrow(ApiException::notFound);
        return permissions.findCodesForRole(role);
    }
    public void assignment(UUID actor, UUID hotel, UUID membership, UUID role, boolean grant) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.ROLE_MANAGE);
        requireMembership(hotel,membership);
        roles.findByIdAndHotel_Id(role,hotel).orElseThrow(ApiException::notFound);
        if(grant) assignments.assignRoleToMembership(hotel,membership,role);
        else assignments.removeRoleFromMembership(hotel,membership,role);
        audit.record(hotel,null,actor,grant?"ROLE_ASSIGNED":"ROLE_REMOVED","MEMBERSHIP",membership);
    }
    private Role requireCustomRole(UUID hotel, UUID role) {
        var result = roles.findByIdAndHotel_Id(role,hotel).orElseThrow(ApiException::notFound);
        if(result.isSystemDefined()) throw new ApiException(409,"SYSTEM_ROLE_PROTECTED","System roles cannot be edited.");
        return result;
    }
    private void requireMembership(UUID hotel, UUID membership) {
        membershipRepository.findLockedInHotel(membership,hotel).orElseThrow(ApiException::notFound);
    }
    private PageRequest page(int page, int size) {
        if(page<0 || size<1 || size>100) throw new IllegalArgumentException("Invalid page.");
        return PageRequest.of(page,size,Sort.by("createdAt","id"));
    }
    private OffsetDateTime now() { return OffsetDateTime.now(ZoneOffset.UTC); }
}
