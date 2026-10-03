package com.hotelmanagement.hms.identity.authentication.web;

import com.hotelmanagement.hms.identity.authentication.service.PasswordRecoveryService;
import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import com.hotelmanagement.hms.shared.web.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class PasswordRecoveryController {
    private final PasswordRecoveryService service;
    private final AuthenticatedUserContext user;
    public PasswordRecoveryController(PasswordRecoveryService service, AuthenticatedUserContext user) { this.service=service; this.user=user; }
    public record Forgot(@NotBlank @Email @Size(max=255) String email) {}
    public record Reset(@NotBlank @Size(max=128) String token, @NotNull @Size(max=256) String newPassword,
                        @NotNull @Size(max=256) String confirmPassword) {
        @Override public String toString() { return "Reset[REDACTED]"; }
    }
    @PostMapping("/api/v1/auth/forgot-password")
    public Map<String,String> forgot(@Valid @RequestBody Forgot request) {
        service.request(request.email());
        return Map.of("message","If an account exists for the supplied information, password reset instructions have been sent.");
    }
    @PostMapping("/api/v1/auth/reset-password") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody Reset request) {
        if (!service.reset(request.token(),request.newPassword(),request.confirmPassword()))
            throw new ApiException(400,"INVALID_RESET_TOKEN","This reset link is invalid or expired. Request a new link.");
    }
    @PostMapping("/api/v1/hotels/{hotel}/memberships/{membership}/password-recovery")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void recover(@PathVariable UUID hotel,@PathVariable UUID membership) {
        service.requestForMember(user.requireCurrentUserId(),hotel,membership);
    }
}
