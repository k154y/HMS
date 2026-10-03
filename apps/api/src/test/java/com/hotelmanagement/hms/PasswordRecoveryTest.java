package com.hotelmanagement.hms;

import com.hotelmanagement.hms.identity.authentication.dto.*;
import com.hotelmanagement.hms.identity.authentication.service.*;
import com.hotelmanagement.hms.identity.authentication.password.PasswordRecoveryNotifier;
import com.hotelmanagement.hms.identity.authentication.password.PasswordRecoveryNotifier.Delivery;
import com.hotelmanagement.hms.identity.authentication.session.security.RefreshTokenGenerator;
import com.hotelmanagement.hms.identity.repository.UserRepository;
import com.hotelmanagement.hms.platform.dto.CreateHotelRequest;
import com.hotelmanagement.hms.platform.onboarding.dto.*;
import com.hotelmanagement.hms.platform.onboarding.service.OwnerOnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.event.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "hms.security.jwt.secret=recovery-integration-only-secret-at-least-32-bytes",
        "hms.security.recovery.enabled=true","logging.level.root=WARN",
        "logging.level.com.hotelmanagement.hms=WARN","management.otlp.metrics.export.enabled=false"})
@RecordApplicationEvents
class PasswordRecoveryTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){IntegrationServices.configure(r);}
    // Never deliver test credentials through a real transport.
    @MockitoBean PasswordRecoveryNotifier notifier;
    @Autowired PasswordRecoveryService recovery;
    @Autowired PasswordChangeService changes;
    @Autowired AuthenticationService auth;
    @Autowired OwnerOnboardingService onboarding;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate db;
    @Autowired ApplicationEvents events;
    @Autowired RefreshTokenGenerator generator;
    @Autowired StringRedisTemplate redis;
    @Autowired org.springframework.web.context.WebApplicationContext web;
    @Autowired tools.jackson.databind.json.JsonMapper json;
    static final String OLD="the old secure passphrase";
    static final String NEW="the new secure passphrase";
    OnboardingResponse owner(){String id=UUID.randomUUID().toString();return onboarding.signup(new OwnerSignupRequest(id+"@example.test",OLD,"Owner",null,"en",new CreateHotelRequest(id,"Hotel","Hotel",null,null,null,null,"RWF","Africa/Kigali","en")));}
    String email(OnboardingResponse o){return users.findById(o.ownerUserId()).orElseThrow().getEmail();}
    String request(OnboardingResponse o){recovery.request(email(o));return events.stream(Delivery.class).filter(d->d.email().equals(email(o))&&d.token()!=null).reduce((a,b)->b).orElseThrow().token();}
    String seed(OnboardingResponse o,OffsetDateTime expiry){var t=generator.generate();db.update("insert into password_reset_tokens(id,user_id,token_hash,created_at,expires_at) values(?,?,?,?,?)",UUID.randomUUID(),o.ownerUserId(),t.tokenHash(),expiry.minusMinutes(20),expiry);return t.rawToken();}
    void clearRecoveryCooldown(OnboardingResponse o){redis.delete("hms:recovery:"+generator.hash(email(o)));}
    org.springframework.test.web.servlet.MockMvc mvc(){return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web).apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();}

    @Test void secondRecoveryRequestInvalidatesFirstAndSecondWorks(){
        var o=owner();

        String first=request(o);
        clearRecoveryCooldown(o);
        String second=request(o);

        assertNotEquals(first,second);
        assertFalse(recovery.reset(first,NEW,NEW));
        assertTrue(recovery.reset(second,NEW,NEW));
    }

    @Test void cleanupRemovesOldTokensButKeepsActiveToken(){
        var now=OffsetDateTime.now(ZoneOffset.UTC);

        var activeOwner=owner();
        String active=seed(activeOwner,now.plusMinutes(10));

        var expiredOwner=owner();
        String expired=seed(expiredOwner,now.minusDays(8));

        var usedOwner=owner();
        var used=generator.generate();

        db.update("""
                insert into password_reset_tokens(
                    id,user_id,token_hash,created_at,expires_at,used_at
                ) values(?,?,?,?,?,?)
                """,
                UUID.randomUUID(),
                usedOwner.ownerUserId(),
                used.tokenHash(),
                now.minusDays(9),
                now.plusDays(1),
                now.minusDays(8));

        var trigger=owner();
        recovery.request(email(trigger));

        assertEquals(1,db.queryForObject(
                "select count(*) from password_reset_tokens where token_hash=?",
                Integer.class,
                generator.hash(active)));

        assertEquals(0,db.queryForObject(
                "select count(*) from password_reset_tokens where token_hash=?",
                Integer.class,
                generator.hash(expired)));

        assertEquals(0,db.queryForObject(
                "select count(*) from password_reset_tokens where token_hash=?",
                Integer.class,
                used.tokenHash()));
    }

    @Test void completeResetChangesLoginRevokesSessionsAndNeverStoresRawToken(){
        var o=owner();var session=auth.login(new LoginRequest(email(o),OLD));String raw=request(o);
        assertNotEquals(raw,db.queryForObject("select token_hash from password_reset_tokens where user_id=?",String.class,o.ownerUserId()));
        String other=seed(o,OffsetDateTime.now().plusMinutes(10));
        assertTrue(recovery.reset(raw,NEW,NEW));
        assertFalse(recovery.reset(raw,NEW,NEW));assertFalse(recovery.reset(other,NEW,NEW));
        assertThrows(BadCredentialsException.class,()->auth.login(new LoginRequest(email(o),OLD)));
        assertNotNull(auth.login(new LoginRequest(email(o),NEW)));
        assertThrows(BadCredentialsException.class,()->auth.refresh(new RefreshTokenRequest(session.refreshToken())));
        var audit=db.queryForList("select action,old_value,new_value,reason from audit_events where entity_id=?",o.ownerUserId()).toString();
        assertTrue(audit.contains("PASSWORD_RESET_COMPLETED"));assertTrue(audit.contains("SESSIONS_REVOKED"));
        assertFalse(audit.contains(raw));assertFalse(audit.contains(NEW));assertFalse(audit.contains(generator.hash(raw)));
    }
    @Test void invalidExpiredAndUsedTokensCannotChangePassword(){
        var o=owner();String raw=seed(o,OffsetDateTime.now().minusMinutes(1));
        assertFalse(recovery.reset(raw,NEW,NEW));assertFalse(recovery.reset("missing-token",NEW,NEW));
        String valid=seed(o,OffsetDateTime.now().plusMinutes(10));
        assertThrows(IllegalArgumentException.class,()->recovery.reset(valid,"short","short"));
        assertThrows(IllegalArgumentException.class,()->recovery.reset(valid,NEW,OLD));
        assertTrue(recovery.reset(valid,NEW,NEW));assertFalse(recovery.reset(valid,NEW,NEW));
    }
    @Test void concurrentConsumptionHasOneWinner()throws Exception{
        var o=owner();String raw=seed(o,OffsetDateTime.now().plusMinutes(10));var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)){
            Callable<Boolean> reset=()->{start.await();return recovery.reset(raw,NEW,NEW);};
            var a=executor.submit(reset);var b=executor.submit(reset);start.countDown();
            assertNotEquals(a.get(45,TimeUnit.SECONDS),b.get(45,TimeUnit.SECONDS));
        }
    }
    @Test void publicResponsesDoNotRevealAccountsOrTokens()throws Exception{
        var o=owner();String path="/api/v1/auth/forgot-password";
        var first=mvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).servletPath(path).with(req->{req.setRemoteAddr("192.0.2.10");return req;}).contentType("application/json").content(json.writeValueAsBytes(Map.of("email",email(o))))).andReturn().getResponse();
        var missing=mvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).servletPath(path).with(req->{req.setRemoteAddr("192.0.2.11");return req;}).contentType("application/json").content(json.writeValueAsBytes(Map.of("email",UUID.randomUUID()+"@example.test")))).andReturn().getResponse();
        assertEquals(200,first.getStatus());assertEquals(first.getStatus(),missing.getStatus());assertEquals(first.getContentAsString(),missing.getContentAsString());
        assertFalse(first.getContentAsString().contains("token"));
        recovery.request(email(o));assertEquals(1,db.queryForObject("select count(*) from password_reset_tokens where user_id=?",Integer.class,o.ownerUserId()));
    }
    @Test void administratorRecoveryRequiresPermissionAndSameHotel(){
        var a=owner();var b=owner();UUID member=db.queryForObject("select id from hotel_memberships where user_id=? and hotel_id=?",UUID.class,a.ownerUserId(),a.hotel().id());
        assertThrows(AccessDeniedException.class,()->recovery.requestForMember(b.ownerUserId(),a.hotel().id(),member));
        assertThrows(com.hotelmanagement.hms.shared.web.ApiException.class,()->recovery.requestForMember(b.ownerUserId(),b.hotel().id(),member));
        recovery.requestForMember(a.ownerUserId(),a.hotel().id(),member);
        assertEquals(1,db.queryForObject("select count(*) from audit_events where action='ADMIN_PASSWORD_RESET_REQUESTED' and entity_id=?",Integer.class,a.ownerUserId()));
        db.update("delete from membership_roles where membership_id=?",member);
        assertThrows(AccessDeniedException.class,()->recovery.requestForMember(a.ownerUserId(),a.hotel().id(),member));
    }
    @Test void disabledAndAdministrativeLocksCannotBeRecovered(){
        var o=owner();String raw=seed(o,OffsetDateTime.now().plusMinutes(10));
        db.update("update users set status='DISABLED' where id=?",o.ownerUserId());recovery.request(email(o));assertFalse(recovery.reset(raw,NEW,NEW));
        db.update("update users set status='LOCKED',locked_until=null where id=?",o.ownerUserId());assertFalse(recovery.reset(raw,NEW,NEW));
    }
    @Test void changePasswordValidatesConfirmationAndPolicy(){
        var o=owner();assertThrows(IllegalArgumentException.class,()->changes.change(o.ownerUserId(),new ChangePasswordRequest(OLD,NEW,OLD)));
        assertThrows(IllegalArgumentException.class,()->changes.change(o.ownerUserId(),new ChangePasswordRequest(OLD,"short","short")));
        assertThrows(BadCredentialsException.class,()->changes.change(o.ownerUserId(),new ChangePasswordRequest("wrong",NEW,NEW)));
        var session=auth.login(new LoginRequest(email(o),OLD));changes.change(o.ownerUserId(),new ChangePasswordRequest(OLD,NEW,NEW));
        assertNotNull(auth.login(new LoginRequest(email(o),NEW)));assertThrows(BadCredentialsException.class,()->auth.refresh(new RefreshTokenRequest(session.refreshToken())));
    }
}
