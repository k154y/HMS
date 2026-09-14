package com.hotelmanagement.hms.platform.onboarding.service;

import com.hotelmanagement.hms.identity.authorization.repository.RoleRepository;
import com.hotelmanagement.hms.identity.authorization.service.RoleAuthorizationService;
import com.hotelmanagement.hms.identity.membership.service.HotelMembershipService;
import com.hotelmanagement.hms.identity.model.UserStatus;
import com.hotelmanagement.hms.identity.repository.UserRepository;
import com.hotelmanagement.hms.identity.service.UserAccountService;
import com.hotelmanagement.hms.platform.dto.CreateHotelRequest;
import com.hotelmanagement.hms.platform.onboarding.dto.*;
import com.hotelmanagement.hms.platform.service.PlatformService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.UUID;

@Service
@Validated
public class OwnerOnboardingService {
    private final UserAccountService accounts;
    private final UserRepository users;
    private final PlatformService platform;
    private final HotelMembershipService memberships;
    private final RoleRepository roles;
    private final RoleAuthorizationService assignments;
    private final com.hotelmanagement.hms.audit.service.AuditService audit;

    public OwnerOnboardingService(UserAccountService accounts, UserRepository users, PlatformService platform,
            HotelMembershipService memberships, RoleRepository roles, RoleAuthorizationService assignments,
            com.hotelmanagement.hms.audit.service.AuditService audit) {
        this.accounts = accounts; this.users = users; this.platform = platform;
        this.memberships = memberships; this.roles = roles; this.assignments = assignments;
        this.audit = audit;
    }

    // One transaction owns identity, hotel, default roles, membership and OWNER assignment.
    @Transactional
    public OnboardingResponse signup(@Valid @NotNull OwnerSignupRequest request) {
        var owner = accounts.createAccount(request.email(), request.password(), request.fullName(),
                request.phone(), request.preferredLanguage());
        return provision(owner.getId(), request.hotel());
    }

    @Transactional
    public OnboardingResponse createForExistingOwner(UUID authenticatedUserId, @Valid @NotNull CreateHotelRequest request) {
        var owner = users.findLockedById(authenticatedUserId)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new BadCredentialsException("Authentication failed."));
        return provision(owner.getId(), request);
    }

    private OnboardingResponse provision(UUID ownerId, CreateHotelRequest request) {
        var hotel = platform.createHotel(request);
        var membership = memberships.createMembership(ownerId, hotel.id(), true);
        var role = roles.findByHotel_IdAndCode(hotel.id(), "OWNER")
                .orElseThrow(() -> new IllegalStateException("Owner role provisioning failed."));
        assignments.assignRoleToMembership(hotel.id(), membership.id(), role.getId());
        audit.record(hotel.id(), null, ownerId, "HOTEL_ONBOARDED", "HOTEL", hotel.id());
        return new OnboardingResponse(ownerId, membership.id(), hotel);
    }
}
