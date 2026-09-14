package com.hotelmanagement.hms.identity.authentication.web;

import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import com.hotelmanagement.hms.identity.authentication.dto.AuthenticationResponse;
import com.hotelmanagement.hms.identity.authentication.dto.LoginRequest;
import com.hotelmanagement.hms.identity.authentication.dto.RefreshTokenRequest;
import com.hotelmanagement.hms.identity.authentication.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService
            authenticationService;

    private final AuthenticatedUserContext
            authenticatedUserContext;

    public AuthenticationController(
            AuthenticationService authenticationService,
            AuthenticatedUserContext authenticatedUserContext) {

        this.authenticationService =
                authenticationService;

        this.authenticatedUserContext =
                authenticatedUserContext;
    }

    /**
     * Authenticates an account and creates:
     *
     * - a short-lived JWT access token;
     * - a rotatable refresh session.
     */
    @PostMapping("/login")
    public AuthenticationResponse login(
            @Valid
            @RequestBody
            LoginRequest request) {

        return authenticationService.login(
                request);
    }

    /**
     * Rotates a valid refresh token and returns a new:
     *
     * - access token;
     * - refresh token.
     *
     * The previously supplied refresh token becomes invalid.
     */
    @PostMapping("/refresh")
    public AuthenticationResponse refresh(
            @Valid
            @RequestBody
            RefreshTokenRequest request) {

        return authenticationService.refresh(
                request);
    }

    /**
     * Logs out the current session.
     *
     * This endpoint is protected by Spring Security.
     *
     * The authenticated user ID comes from the validated JWT,
     * not from request JSON.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid
            @RequestBody
            RefreshTokenRequest request) {

        UUID currentUserId =
                authenticatedUserContext
                        .requireCurrentUserId();

        authenticationService.logout(
                currentUserId,
                request
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}