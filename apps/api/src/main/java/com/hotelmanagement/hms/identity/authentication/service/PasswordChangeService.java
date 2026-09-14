package com.hotelmanagement.hms.identity.authentication.service;

import com.hotelmanagement.hms.identity.authentication.dto.ChangePasswordRequest;
import com.hotelmanagement.hms.identity.authentication.password.PasswordPolicy;
import com.hotelmanagement.hms.identity.authentication.session.service.RefreshSessionService;
import com.hotelmanagement.hms.identity.model.UserStatus;
import com.hotelmanagement.hms.identity.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class PasswordChangeService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordPolicy policy;
    private final RefreshSessionService sessions;
    private final com.hotelmanagement.hms.audit.service.AuditService audit;
    public PasswordChangeService(UserRepository users, PasswordEncoder encoder, PasswordPolicy policy,
                                 RefreshSessionService sessions, com.hotelmanagement.hms.audit.service.AuditService audit) {
        this.users = users; this.encoder = encoder; this.policy = policy; this.sessions = sessions;
        this.audit = audit;
    }
    @Transactional
    public void change(UUID userId, ChangePasswordRequest request) {
        var user = users.findLockedById(userId).orElseThrow(() -> new BadCredentialsException("Authentication failed."));
        if (user.getStatus() != UserStatus.ACTIVE || !encoder.matches(request.currentPassword(), user.getPasswordHash()))
            throw new BadCredentialsException("Authentication failed.");
        policy.validateNewPassword(request.newPassword());
        user.changePassword(encoder.encode(request.newPassword()), OffsetDateTime.now(ZoneOffset.UTC));
        sessions.revokeAllForUser(userId);
        users.flush();
        audit.record(null, null, userId, "PASSWORD_CHANGED", "USER", userId);
    }
}
