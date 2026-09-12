package com.hotelmanagement.hms.identity.authorization.service;

import com.hotelmanagement.hms.identity.authorization.decision.AuthorizationDecision;
import com.hotelmanagement.hms.identity.authorization.model.PermissionCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthorizationGuard {

    private final TenantAuthorizationService
            authorizationService;

    public AuthorizationGuard(
            TenantAuthorizationService authorizationService) {

        this.authorizationService =
                authorizationService;
    }

    /**
     * Requires hotel-level authorization.
     *
     * Use for operations that are not tied to one branch, for example:
     *
     * - exchange-rate management;
     * - hotel settings;
     * - hotel membership administration;
     * - role administration.
     */
    public void requireHotelPermission(
            UUID userId,
            UUID hotelId,
            PermissionCode permission) {

        requirePermission(
                userId,
                hotelId,
                null,
                permission
        );
    }

    /**
     * Requires authorization within a specific branch.
     *
     * Use for branch-scoped operations such as:
     *
     * - reservations;
     * - room operations;
     * - cashier shifts;
     * - inventory;
     * - POS orders.
     */
    public void requireBranchPermission(
            UUID userId,
            UUID hotelId,
            UUID branchId,
            PermissionCode permission) {

        if (branchId == null) {
            throw new IllegalArgumentException(
                    "Branch ID is required "
                            + "for branch-scoped authorization.");
        }

        requirePermission(
                userId,
                hotelId,
                branchId,
                permission
        );
    }

    private void requirePermission(
            UUID userId,
            UUID hotelId,
            UUID branchId,
            PermissionCode permission) {

        AuthorizationDecision decision =
                authorizationService.authorize(
                        userId,
                        hotelId,
                        branchId,
                        permission
                );

        if (!decision.allowed()) {

            /*
             * Keep the external message generic.
             *
             * Detailed denial reasons remain available internally
             * through AuthorizationDecision for controlled logging,
             * diagnostics, and tests.
             */
            throw new AccessDeniedException(
                    "Access denied."
            );
        }
    }
}