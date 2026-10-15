package com.hotelmanagement.hms.platform.onboarding.web;

import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import com.hotelmanagement.hms.platform.dto.CreateHotelRequest;
import com.hotelmanagement.hms.platform.onboarding.dto.*;
import com.hotelmanagement.hms.platform.onboarding.service.OwnerOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding")
public class OwnerOnboardingController {
    private final OwnerOnboardingService service;
    private final AuthenticatedUserContext user;
    public OwnerOnboardingController(OwnerOnboardingService service, AuthenticatedUserContext user) {
        this.service = service; this.user = user;
    }
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardingResponse signup(@Valid @RequestBody OwnerSignupRequest request) {
        return service.signup(request);
    }
    @PostMapping("/hotels")
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardingResponse createHotel(@Valid @RequestBody CreateHotelRequest request) {
        return service.createForExistingOwner(user.requireCurrentUserId(), request);
    }
}
