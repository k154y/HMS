package com.hotelmanagement.hms.identity.authentication.service;

import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.identity.authentication.config.PasswordRecoveryProperties;
import com.hotelmanagement.hms.identity.authentication.password.PasswordPolicy;
import com.hotelmanagement.hms.identity.authentication.password.PasswordRecoveryNotifier.Delivery;
import com.hotelmanagement.hms.identity.authentication.session.security.RefreshTokenGenerator;
import com.hotelmanagement.hms.identity.authentication.session.service.RefreshSessionService;
import com.hotelmanagement.hms.identity.authorization.service.AuthorizationGuard;
import com.hotelmanagement.hms.identity.authorization.model.PermissionCode;
import com.hotelmanagement.hms.identity.membership.repository.HotelMembershipRepository;
import com.hotelmanagement.hms.identity.model.UserAccount;
import com.hotelmanagement.hms.identity.model.UserStatus;
import com.hotelmanagement.hms.identity.repository.UserRepository;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class PasswordRecoveryService {
    private static final Duration RESET_TOKEN_RETENTION = Duration.ofDays(7);

    private final UserRepository users;
    private final JdbcTemplate db;
    private final RefreshTokenGenerator tokens;
    private final RefreshSessionService sessions;
    private final PasswordEncoder encoder;
    private final PasswordPolicy policy;
    private final AuditService audit;
    private final StringRedisTemplate redis;
    private final ApplicationEventPublisher events;
    private final PasswordRecoveryProperties config;
    private final AuthorizationGuard guard;
    private final HotelMembershipRepository memberships;
    public PasswordRecoveryService(UserRepository users, JdbcTemplate db, RefreshTokenGenerator tokens,
            RefreshSessionService sessions, PasswordEncoder encoder, PasswordPolicy policy, AuditService audit,
            StringRedisTemplate redis, ApplicationEventPublisher events, PasswordRecoveryProperties config,
            AuthorizationGuard guard, HotelMembershipRepository memberships) {
        this.users=users; this.db=db; this.tokens=tokens; this.sessions=sessions; this.encoder=encoder;
        this.policy=policy; this.audit=audit; this.redis=redis; this.events=events; this.config=config;
        this.guard=guard; this.memberships=memberships;
    }
    @Transactional
    public void request(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!config.enabled() || !allow(normalized)) return;
        users.findLockedByEmail(normalized).filter(this::eligible).ifPresent(user -> issue(user, null, null));
    }
    @Transactional
    public void requestForMember(UUID actor, UUID hotel, UUID membership) {
        guard.requireHotelPermission(actor, hotel, PermissionCode.USER_MANAGE);
        var member = memberships.findByIdAndHotel_Id(membership, hotel).orElseThrow(ApiException::notFound);
        if (!config.enabled()) throw new ApiException(503,"RECOVERY_UNAVAILABLE","Account recovery is not configured.");
        var user = users.findLockedById(member.getUser().getId()).orElseThrow(ApiException::notFound);
        if (eligible(user) && allow(user.getNormalizedEmail())) issue(user, actor, hotel);
    }
    private boolean eligible(UserAccount user) {
        return user.getStatus() == UserStatus.ACTIVE
                || (user.getStatus() == UserStatus.LOCKED && user.getLockedUntil() != null);
    }
    private boolean allow(String email) {
        try {
            // Apply equally to missing accounts, and share cooldown across public/admin requests.
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("hms:recovery:" + tokens.hash(email), "1", Duration.ofMinutes(1)));
        } catch (org.springframework.dao.DataAccessException failure) {
            throw new ApiException(503,"AUTH_LIMITER_UNAVAILABLE","Please retry later.");
        }
    }
    private void issue(UserAccount user, UUID actor, UUID hotel) {
        var token=tokens.generate(); var now=OffsetDateTime.now(ZoneOffset.UTC);
        cleanupOldTokens(now);
        db.update("update password_reset_tokens set used_at=? where user_id=? and used_at is null",now,user.getId());
        db.update("insert into password_reset_tokens(id,user_id,token_hash,expires_at,created_at) values(?,?,?,?,?)",
                UUID.randomUUID(),user.getId(),token.tokenHash(),now.plusMinutes(20),now);
        audit.record(hotel,null,actor,actor==null?"PASSWORD_RESET_REQUESTED":"ADMIN_PASSWORD_RESET_REQUESTED","USER",user.getId());
        events.publishEvent(new Delivery(user.getEmail(),token.rawToken()));
    }
    private void cleanupOldTokens(OffsetDateTime now) {
        var cutoff=now.minus(RESET_TOKEN_RETENTION);

        db.update("""
                delete from password_reset_tokens
                where (used_at is not null and used_at < ?)
                   or (used_at is null and expires_at < ?)
                """,cutoff,cutoff);
    }

    @Transactional
    public boolean reset(String rawToken, String password, String confirmation) {
        policy.validateNewPassword(password);
        if (!Objects.equals(password,confirmation)) throw new IllegalArgumentException("Passwords do not match.");
        String hash=tokens.hash(rawToken);
        var ids=db.queryForList("select user_id from password_reset_tokens where token_hash=?",UUID.class,hash);
        if (ids.isEmpty()) return failed(null);
        // Same lock order as login, refresh and password change. Simultaneous consumption serializes here.
        var user=users.findLockedById(ids.getFirst()).orElseThrow(ApiException::notFound);
        var now=OffsetDateTime.now(ZoneOffset.UTC);
        if (!eligible(user)) return failed(user.getId());
        int consumed=db.update("update password_reset_tokens set used_at=? where token_hash=? and used_at is null and expires_at>?",now,hash,now);
        if (consumed != 1) return failed(user.getId());
        user.changePassword(encoder.encode(password),now); users.flush();
        db.update("update password_reset_tokens set used_at=? where user_id=? and used_at is null",now,user.getId());
        sessions.revokeAllForUser(user.getId());
        audit.record(null,null,user.getId(),"PASSWORD_RESET_COMPLETED","USER",user.getId());
        audit.record(null,null,user.getId(),"SESSIONS_REVOKED","USER",user.getId());
        events.publishEvent(new Delivery(user.getEmail(),null));
        return true;
    }
    private boolean failed(UUID user) {
        audit.record(null,null,null,"PASSWORD_RESET_FAILED",user==null?"RECOVERY_ATTEMPT":"USER",user==null?UUID.randomUUID():user);
        return false;
    }
}
