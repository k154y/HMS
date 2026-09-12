package com.hotelmanagement.hms.identity.authentication.context;

import com.hotelmanagement.hms.identity.authentication.model.AuthenticatedUser;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthenticatedUserContext {

    /**
     * Returns the identity represented by the validated JWT
     * currently stored in Spring Security's context.
     */
    public AuthenticatedUser requireCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication is required.");
        }

        if (!(authentication
                instanceof JwtAuthenticationToken jwtAuthentication)) {

            throw new AuthenticationCredentialsNotFoundException(
                    "JWT authentication is required.");
        }

        String subject =
                jwtAuthentication
                        .getToken()
                        .getSubject();

        if (subject == null
                || subject.isBlank()) {

            throw new AuthenticationCredentialsNotFoundException(
                    "JWT subject is missing.");
        }

        UUID userId;

        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {

            throw new AuthenticationCredentialsNotFoundException(
                    "JWT subject is invalid.",
                    exception
            );
        }

        String email =
                jwtAuthentication
                        .getToken()
                        .getClaimAsString("email");

        return new AuthenticatedUser(
                userId,
                email
        );
    }

    public UUID requireCurrentUserId() {
        return requireCurrentUser()
                .userId();
    }
}