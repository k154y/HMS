package com.hotelmanagement.hms.identity.authorization.service;

import com.hotelmanagement.hms.identity.authorization.decision.AuthorizationDecision;
import com.hotelmanagement.hms.identity.authorization.decision.AuthorizationDenialReason;
import com.hotelmanagement.hms.identity.authorization.model.PermissionCode;
import com.hotelmanagement.hms.identity.authorization.repository.RolePermissionRepository;
import com.hotelmanagement.hms.identity.membership.model.HotelMembership;
import com.hotelmanagement.hms.identity.membership.model.HotelMembershipStatus;
import com.hotelmanagement.hms.identity.membership.repository.HotelMembershipRepository;
import com.hotelmanagement.hms.identity.membership.repository.MembershipBranchAccessRepository;
import com.hotelmanagement.hms.identity.model.UserAccount;
import com.hotelmanagement.hms.identity.model.UserStatus;
import com.hotelmanagement.hms.identity.repository.UserRepository;
import com.hotelmanagement.hms.platform.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TenantAuthorizationService {

    private final UserRepository userRepository;

    private final HotelMembershipRepository
            membershipRepository;

    private final MembershipBranchAccessRepository
            branchAccessRepository;

    private final BranchRepository branchRepository;

    private final RolePermissionRepository
            rolePermissionRepository;

    public TenantAuthorizationService(
            UserRepository userRepository,
            HotelMembershipRepository membershipRepository,
            MembershipBranchAccessRepository branchAccessRepository,
            BranchRepository branchRepository,
            RolePermissionRepository rolePermissionRepository) {

        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.branchAccessRepository = branchAccessRepository;
        this.branchRepository = branchRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * Evaluates tenant-level authorization.
     *
     * branchId may be null for hotel-wide operations such as:
     *
     * - managing exchange rates;
     * - viewing hotel settings;
     * - managing hotel roles.
     *
     * For branch-specific operations such as reservations, rooms,
     * cashier shifts, inventory, and POS activity, branchId should
     * be supplied.
     */
    @Transactional(readOnly = true)
    public AuthorizationDecision authorize(
            UUID userId,
            UUID hotelId,
            UUID branchId,
            PermissionCode requiredPermission) {

        if (userId == null) {
            return AuthorizationDecision.deny(
                    AuthorizationDenialReason.USER_NOT_FOUND
            );
        }

        if (hotelId == null) {
            return AuthorizationDecision.deny(
                    AuthorizationDenialReason.MEMBERSHIP_NOT_FOUND
            );
        }

        if (requiredPermission == null) {
            return AuthorizationDecision.deny(
                    AuthorizationDenialReason.PERMISSION_DENIED
            );
        }

        UserAccount user =
                userRepository.findById(userId)
                        .orElse(null);

        if (user == null) {
            return AuthorizationDecision.deny(
                    AuthorizationDenialReason.USER_NOT_FOUND
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            return AuthorizationDecision.deny(
                    AuthorizationDenialReason.ACCOUNT_NOT_ACTIVE
            );
        }

        HotelMembership membership =
                membershipRepository
                        .findByUser_IdAndHotel_Id(
                                userId,
                                hotelId)
                        .orElse(null);

        if (membership == null) {
            return AuthorizationDecision.deny(
                    AuthorizationDenialReason.MEMBERSHIP_NOT_FOUND
            );
        }

        if (membership.getStatus()
                != HotelMembershipStatus.ACTIVE) {

            return AuthorizationDecision.deny(
                    AuthorizationDenialReason.MEMBERSHIP_NOT_ACTIVE
            );
        }

        if (branchId != null) {

            boolean branchBelongsToHotel =
                    branchRepository
                            .findByIdAndHotel_Id(
                                    branchId,
                                    hotelId)
                            .isPresent();

            if (!branchBelongsToHotel) {
                return AuthorizationDecision.deny(
                        AuthorizationDenialReason.BRANCH_NOT_FOUND
                );
            }

            if (!membership.hasAllBranches()) {

                boolean branchAllowed =
                        branchAccessRepository
                                .existsByMembership_IdAndBranch_Id(
                                        membership.getId(),
                                        branchId
                                );

                if (!branchAllowed) {
                    return AuthorizationDecision.deny(
                            AuthorizationDenialReason.BRANCH_ACCESS_DENIED
                    );
                }
            }
        }

        List<String> permissionCodes =
                rolePermissionRepository
                        .findPermissionCodesForMembership(
                                membership.getId(),
                                hotelId
                        );

        boolean hasPermission =
                permissionCodes.contains(
                        requiredPermission.name());

        if (!hasPermission) {
            return AuthorizationDecision.deny(
                    AuthorizationDenialReason.PERMISSION_DENIED
            );
        }

        return AuthorizationDecision.allow();
    }
}