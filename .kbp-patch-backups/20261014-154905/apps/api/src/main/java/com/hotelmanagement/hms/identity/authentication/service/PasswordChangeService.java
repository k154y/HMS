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
    private final org.springframework.jdbc.core.JdbcTemplate db;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordPolicy policy;
    private final RefreshSessionService sessions;
    private final com.hotelmanagement.hms.audit.service.AuditService audit;
    public PasswordChangeService(UserRepository users, PasswordEncoder encoder, PasswordPolicy policy,
                                 RefreshSessionService sessions, com.hotelmanagement.hms.audit.service.AuditService audit,org.springframework.jdbc.core.JdbcTemplate db,org.springframework.context.ApplicationEventPublisher events) {
        this.db=db;this.events=events;
        this.users = users; this.encoder = encoder; this.policy = policy; this.sessions = sessions;
        this.audit = audit;
    }
    @Transactional
    public void change(UUID userId, ChangePasswordRequest request) {
        var user = users.findLockedById(userId).orElseThrow(() -> new BadCredentialsException("Authentication failed."));
        if (user.getStatus() != UserStatus.ACTIVE || !encoder.matches(request.currentPassword(), user.getPasswordHash()))
            throw new BadCredentialsException("Authentication failed.");
        policy.validateNewPassword(request.newPassword());
        if (request.confirmPassword() != null && !request.newPassword().equals(request.confirmPassword()))
            throw new IllegalArgumentException("Passwords do not match.");
        user.changePassword(encoder.encode(request.newPassword()), OffsetDateTime.now(ZoneOffset.UTC));
        sessions.revokeAllForUser(userId);
        db.update("update password_reset_tokens set used_at=current_timestamp where user_id=? and used_at is null",userId);
        events.publishEvent(new com.hotelmanagement.hms.identity.authentication.password.PasswordRecoveryNotifier.Delivery(user.getEmail(),null));
        users.flush();
        audit.record(null, null, userId, "SESSIONS_REVOKED", "USER", userId);
        audit.record(null, null, userId, "PASSWORD_CHANGED", "USER", userId);
    }
}
