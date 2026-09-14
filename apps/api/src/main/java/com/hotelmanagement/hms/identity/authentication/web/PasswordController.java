package com.hotelmanagement.hms.identity.authentication.web;

import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import com.hotelmanagement.hms.identity.authentication.dto.ChangePasswordRequest;
import com.hotelmanagement.hms.identity.authentication.service.PasswordChangeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class PasswordController {
    private final PasswordChangeService service;
    private final AuthenticatedUserContext user;
    public PasswordController(PasswordChangeService service, AuthenticatedUserContext user) {
        this.service = service; this.user = user;
    }
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void change(@Valid @RequestBody ChangePasswordRequest request) {
        service.change(user.requireCurrentUserId(), request);
    }
}
