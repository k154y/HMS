package com.hotelmanagement.hms;

import com.hotelmanagement.hms.identity.authentication.dto.*;
import com.hotelmanagement.hms.identity.authentication.password.PasswordPolicy;
import com.hotelmanagement.hms.identity.authentication.service.*;
import com.hotelmanagement.hms.identity.authentication.session.repository.RefreshSessionRepository;
import com.hotelmanagement.hms.identity.authorization.model.PermissionCode;
import com.hotelmanagement.hms.identity.authorization.service.AuthorizationGuard;
import com.hotelmanagement.hms.identity.model.UserStatus;
import com.hotelmanagement.hms.identity.repository.UserRepository;
import com.hotelmanagement.hms.platform.dto.CreateHotelRequest;
import com.hotelmanagement.hms.platform.onboarding.dto.*;
import com.hotelmanagement.hms.platform.onboarding.service.OwnerOnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"hms.security.jwt.secret=integration-tests-only-secret-at-least-32-bytes",
        "spring.main.lazy-initialization=false", "logging.level.root=WARN",
        "management.otlp.metrics.export.enabled=false"})

class BackendFoundationTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) { IntegrationServices.configure(r); }
    @Autowired OwnerOnboardingService onboarding;
    @Autowired UserRepository users;
    @Autowired AuthenticationService authentication;
    @Autowired PasswordChangeService passwords;
    @Autowired RefreshSessionRepository sessions;
    @Autowired PasswordEncoder encoder;
    @Autowired AuthorizationGuard guard;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.web.context.WebApplicationContext web;
    @Autowired tools.jackson.databind.json.JsonMapper mapper;
    @Autowired com.hotelmanagement.hms.platform.service.HotelAdministrationService hotelAdmin;
    @Autowired com.hotelmanagement.hms.identity.administration.service.StaffAdministrationService staffAdmin;
    @Autowired com.hotelmanagement.hms.audit.service.AuditService audit;
    @Autowired org.springframework.data.redis.core.StringRedisTemplate redis;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    private static final String PASSWORD = "a long owner passphrase";

    private org.springframework.test.web.servlet.MockMvc mvc() {
        return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web)
                .addFilters(new com.hotelmanagement.hms.shared.web.RequestCorrelationFilter())
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
    }
    private String token(OnboardingResponse owner) {
        return authentication.login(new LoginRequest(users.findById(owner.ownerUserId()).orElseThrow().getEmail(),PASSWORD)).accessToken();
    }
    private int http(String method, String path, String token, Object body) throws Exception {
        var builder = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                org.springframework.http.HttpMethod.valueOf(method),path);
        builder.servletPath(path.split("[?]",2)[0]);
        if(token!=null) builder.header("Authorization","Bearer "+token);
        if(body!=null) builder.contentType("application/json").content(mapper.writeValueAsBytes(body));
        return mvc().perform(builder).andReturn().getResponse().getStatus();
    }
    @Test void securityFiltersEnforceJwtAndTenantAndAccountStatus() throws Exception {
        var a=owner(); var b=owner(); String bearer=token(a);
        String path="/api/v1/hotels/"+a.hotel().id();
        assertEquals(401,http("GET",path,null,null));
        assertEquals(401,http("GET",path,"invalid",null));
        assertEquals(403,http("GET",path,token(b),null));
        assertEquals(200,http("GET",path,bearer,null));
        assertEquals(403,http("GET","/actuator/prometheus",bearer,null));
        jdbc.update("update users set status='DISABLED' where id=?",a.ownerUserId());
        assertEquals(401,http("GET",path,bearer,null));
    }
    @Test void onboardingHttpAndErrorsAreRealAndCorrelated() throws Exception {
        redis.delete("hms:auth-limit:127.0.0.1");
        String id=UUID.randomUUID().toString();
        assertEquals(201,http("POST","/api/v1/onboarding/signup",null,request(id,id+"@example.test")));
        var response=mvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/onboarding/signup")
                .contentType("application/json").content("{}").header("X-Request-ID","invalid\nvalue")).andReturn().getResponse();
        assertEquals(400,response.getStatus());
        var json=mapper.readTree(response.getContentAsString());
        assertEquals(response.getHeader("X-Request-ID"),json.get("requestId").asString());
        assertTrue(json.get("fieldErrors").size()>0);
        var account=owner();
        assertEquals(201,http("POST","/api/v1/onboarding/hotels",token(account),hotel(UUID.randomUUID().toString())));
    }
    @Test void redisThrottleIsAtomicAndBounded() throws Exception {
        redis.opsForValue().set("hms:auth-limit:127.0.0.1","30",java.time.Duration.ofMinutes(1));
        try { assertEquals(429,http("POST","/api/v1/auth/login",null,new LoginRequest("nobody@example.test",PASSWORD))); }
        finally { redis.delete("hms:auth-limit:127.0.0.1"); }
    }

    @Test void realHttpServerHealthAndThrottleWork() throws Exception {
        int port = Integer.parseInt(web.getEnvironment().getProperty("local.server.port"));
        try(var client=java.net.http.HttpClient.newHttpClient()) {
            var health=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(
                    "http://localhost:"+port+"/actuator/health")).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200,health.statusCode());
            assertTrue(health.body().contains("UP"));
            assertTrue(health.headers().firstValue("X-Request-ID").isPresent());
            redis.opsForValue().set("hms:auth-limit:127.0.0.1","30",java.time.Duration.ofMinutes(1));
            try {
                var limited=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(
                        "http://127.0.0.1:"+port+"/api/v1/auth/login")).header("Content-Type","application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}")).build(),
                        java.net.http.HttpResponse.BodyHandlers.ofString());
                assertEquals(429,limited.statusCode());
            } finally { redis.delete("hms:auth-limit:127.0.0.1"); }
        }
    }
    @Test void hotelBranchFxAndAuditApisRespectScope() throws Exception {
        var a=owner(); var b=owner(); String bearer=token(a); String base="/api/v1/hotels/"+a.hotel().id();
        var branch=hotelAdmin.createBranch(a.ownerUserId(),a.hotel().id(),
                new com.hotelmanagement.hms.platform.dto.CreateBranchRequest("HQ","Head office",null,null,null,null));
        String branchPath=base+"/branches/"+branch.id();
        assertEquals(200,http("PUT",base,bearer,hotel(a.hotel().code())));
        assertEquals(200,http("GET",base+"/branches",bearer,null));
        assertEquals(200,http("GET",branchPath,bearer,null));
        assertEquals(403,http("GET",branchPath,token(b),null));
        assertEquals(200,http("PUT",branchPath,bearer,
                new com.hotelmanagement.hms.platform.dto.CreateBranchRequest("HQ","Updated",null,null,null,null)));
        assertEquals(200,http("PUT",branchPath+"/status",bearer,java.util.Map.of("active",false)));
        assertEquals(201,http("POST",base+"/branches",bearer,
                new com.hotelmanagement.hms.platform.dto.CreateBranchRequest("SECOND","Second",null,null,null,null)));
        var at=OffsetDateTime.now().minusMinutes(1);
        assertEquals(201,http("POST",base+"/exchange-rates",bearer,
                new com.hotelmanagement.hms.platform.currency.dto.ExchangeRateRequest("USD",new java.math.BigDecimal("1450"),at)));
        var rate=hotelAdmin.rate(a.ownerUserId(),a.hotel().id(),"USD",at.plusSeconds(1));
        hotelAdmin.createRate(a.ownerUserId(),a.hotel().id(),
                new com.hotelmanagement.hms.platform.currency.dto.ExchangeRateRequest("USD",new java.math.BigDecimal("1500"),at.plusSeconds(10)));
        assertEquals(0,rate.rateToBase().compareTo(hotelAdmin.rate(a.ownerUserId(),a.hotel().id(),"USD",at.plusSeconds(1)).rateToBase()));
        assertEquals(200,http("GET",base+"/exchange-rates",bearer,null));
        assertEquals(200,http("GET",base+"/exchange-rates/applicable?currency=USD",bearer,null));
        assertEquals(200,http("GET",base+"/audit-events",bearer,null));
        assertEquals(403,http("GET",base+"/audit-events",token(b),null));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("delete from audit_events where hotel_id=?",a.hotel().id()));
        assertFalse(audit.list(a.ownerUserId(),a.hotel().id(),0,100).isEmpty());
    }
    @Test void staffRoleApisAndRestrictedBranchListing() throws Exception {
        var a=owner(); var b=owner(); String bearer=token(a); String base="/api/v1/hotels/"+a.hotel().id();
        var staff=staffAdmin.addStaff(a.ownerUserId(),a.hotel().id(),
                new com.hotelmanagement.hms.identity.administration.dto.StaffRequest(b.ownerUserId(),null,null,null,null,null,false));
        var role=staffAdmin.createRole(a.ownerUserId(),a.hotel().id(),
                new com.hotelmanagement.hms.identity.administration.dto.RoleRequest("VIEWER","Viewer",null,true));
        String rolePath=base+"/roles/"+role.id();
        String memberPath=base+"/memberships/"+staff.id();
        assertEquals(200,http("GET",base+"/memberships",bearer,null));
        assertEquals(200,http("GET",base+"/roles",bearer,null));
        assertEquals(200,http("PUT",rolePath,bearer,
                new com.hotelmanagement.hms.identity.administration.dto.RoleRequest("VIEWER","Read only",null,true)));
        assertEquals(204,http("PUT",rolePath+"/permissions/BRANCH_VIEW",bearer,null));
        assertEquals(200,http("GET",rolePath+"/permissions",bearer,null));
        assertEquals(204,http("PUT",memberPath+"/roles/"+role.id(),bearer,null));
        var one=hotelAdmin.createBranch(a.ownerUserId(),a.hotel().id(),
                new com.hotelmanagement.hms.platform.dto.CreateBranchRequest("ONE","One",null,null,null,null));
        var two=hotelAdmin.createBranch(a.ownerUserId(),a.hotel().id(),
                new com.hotelmanagement.hms.platform.dto.CreateBranchRequest("TWO","Two",null,null,null,null));
        assertEquals(204,http("PUT",memberPath+"/branch-access",bearer,
                java.util.Map.of("allBranches",false,"branchIds",java.util.List.of(one.id()))));
        assertEquals(1,hotelAdmin.branches(b.ownerUserId(),a.hotel().id(),0,50).getTotalElements());
        assertThrows(AccessDeniedException.class,()->hotelAdmin.branch(b.ownerUserId(),a.hotel().id(),two.id()));
        assertEquals(204,http("DELETE",rolePath+"/permissions/BRANCH_VIEW",bearer,null));
        assertEquals(204,http("DELETE",memberPath+"/roles/"+role.id(),bearer,null));
        assertEquals(200,http("PUT",memberPath+"/status",bearer,java.util.Map.of("status","SUSPENDED")));
        assertEquals(403,http("GET",base+"/memberships",token(b),null));
        assertEquals(201,http("POST",base+"/roles",bearer,
                new com.hotelmanagement.hms.identity.administration.dto.RoleRequest("CUSTOM","Custom",null,true)));
        String staffEmail=UUID.randomUUID()+"@example.test";
        assertEquals(201,http("POST",base+"/memberships",bearer,
                new com.hotelmanagement.hms.identity.administration.dto.StaffRequest(null,staffEmail,PASSWORD,"Staff",null,"en",false)));
        UUID ownerRole=jdbc.queryForObject("select id from roles where hotel_id=? and code='OWNER'",UUID.class,a.hotel().id());
        assertEquals(409,http("PUT",base+"/roles/"+ownerRole+"/permissions/ROOM_VIEW",bearer,null));
    }
    @Test void logoutAndPasswordHttpWorkAndExpiredRefreshIsRejected() throws Exception {
        redis.delete("hms:auth-limit:127.0.0.1");
        var a=owner(); String email=users.findById(a.ownerUserId()).orElseThrow().getEmail();
        var tokens=authentication.login(new LoginRequest(email,PASSWORD));
        assertEquals(204,http("POST","/api/v1/auth/logout",tokens.accessToken(),new RefreshTokenRequest(tokens.refreshToken())));
        assertThrows(BadCredentialsException.class,()->authentication.refresh(new RefreshTokenRequest(tokens.refreshToken())));
        var next=authentication.login(new LoginRequest(email,PASSWORD));
        jdbc.update("update refresh_sessions set created_at=current_timestamp-interval '2 days',expires_at=current_timestamp-interval '1 day' where user_id=? and revoked_at is null",a.ownerUserId());
        assertThrows(BadCredentialsException.class,()->authentication.refresh(new RefreshTokenRequest(next.refreshToken())));
        assertEquals(204,http("POST","/api/v1/auth/password",next.accessToken(),new ChangePasswordRequest(PASSWORD,"new owner passphrase")));
    }

    private CreateHotelRequest hotel(String code) {
        return new CreateHotelRequest(code, "Legal hotel", "Hotel", null, null, null, null, "RWF", "Africa/Kigali", "en");
    }
    private OwnerSignupRequest request(String code, String email) {
        return new OwnerSignupRequest(email, PASSWORD, "Owner", null, "en", hotel(code));
    }
    private OnboardingResponse owner() {
        String id = UUID.randomUUID().toString();
        return onboarding.signup(request(id, id + "@example.test"));
    }

    @Test void signupCreatesCompleteOwnerAndCalendarTrial() {
        var result = owner();
        var account = users.findById(result.ownerUserId()).orElseThrow();
        assertTrue(encoder.matches(PASSWORD, account.getPasswordHash()));
        assertNotEquals(PASSWORD, account.getPasswordHash());
        assertEquals(result.hotel().trialStartedAt().plusMonths(3), result.hotel().trialEndsAt());
        assertEquals(Boolean.TRUE, jdbc.queryForObject("select all_branches from hotel_memberships where id = ?",
                Boolean.class, result.membershipId()));
        assertEquals(1, jdbc.queryForObject("select count(*) from membership_roles mr join roles r on r.id=mr.role_id where mr.membership_id=? and r.code='OWNER'",
                Integer.class, result.membershipId()));
        guard.requireHotelPermission(result.ownerUserId(), result.hotel().id(), PermissionCode.ROOM_MANAGE);
    }
    @Test void duplicateHotelRollsBackNewIdentity() {
        var first = owner();
        String email = UUID.randomUUID() + "@example.test";
        assertThrows(IllegalStateException.class, () -> onboarding.signup(request(first.hotel().code(), email)));
        assertFalse(users.existsByNormalizedEmail(email));
    }
    @Test void normalizedDuplicateAccountDoesNotCreateHotel() {
        var first = owner();
        String code = UUID.randomUUID().toString();
        String email = users.findById(first.ownerUserId()).orElseThrow().getEmail();
        assertThrows(IllegalStateException.class, () -> onboarding.signup(request(code, email.toUpperCase())));
        assertEquals(0, jdbc.queryForObject("select count(*) from hotels where code=?", Integer.class, code.toUpperCase()));
    }
    @Test void lateAssignmentFailureRollsBackEveryProvisioningStep() {
        // Inject an actual database failure at the last provisioning step.
        jdbc.execute("create function test_reject_owner() returns trigger language plpgsql as $$ begin raise exception 'injected failure'; end $$");
        jdbc.execute("create trigger test_reject_owner before insert on membership_roles for each row execute function test_reject_owner()");
        String code = UUID.randomUUID().toString();
        String email = code + "@example.test";
        try {
            assertThrows(RuntimeException.class, () -> onboarding.signup(request(code, email)));
            assertFalse(users.existsByNormalizedEmail(email));
            assertEquals(0, jdbc.queryForObject("select count(*) from hotels where code=?", Integer.class, code.toUpperCase()));
        } finally {
            jdbc.execute("drop trigger test_reject_owner on membership_roles");
            jdbc.execute("drop function test_reject_owner()");
        }
    }
    @Test void existingOwnerCreatesSecondHotelWithoutDuplicatingIdentity() {
        var first = owner();
        var second = onboarding.createForExistingOwner(first.ownerUserId(), hotel(UUID.randomUUID().toString()));
        assertEquals(first.ownerUserId(), second.ownerUserId());
        assertNotEquals(first.hotel().id(), second.hotel().id());
    }
    @Test void failedAttemptsCommitAndTemporaryLockExpires() {
        var owner = owner();
        String email = users.findById(owner.ownerUserId()).orElseThrow().getEmail();
        for (int n = 0; n < 5; n++)
            assertThrows(BadCredentialsException.class, () -> authentication.login(new LoginRequest(email, "incorrect")));
        var locked = users.findById(owner.ownerUserId()).orElseThrow();
        assertEquals(5, locked.getFailedLoginAttempts());
        assertEquals(UserStatus.LOCKED, locked.getStatus());
        assertThrows(org.springframework.security.authentication.LockedException.class,
                () -> authentication.login(new LoginRequest(email, PASSWORD)));
        jdbc.update("update users set locked_until = current_timestamp - interval '1 minute' where id=?", owner.ownerUserId());
        assertNotNull(authentication.login(new LoginRequest(email, PASSWORD)).accessToken());
        assertEquals(0, users.findById(owner.ownerUserId()).orElseThrow().getFailedLoginAttempts());
    }
    @Test void passwordChangeRevokesEveryRefreshSession() {
        var owner = owner();
        String email = users.findById(owner.ownerUserId()).orElseThrow().getEmail();
        var token = authentication.login(new LoginRequest(email, PASSWORD));
        assertThrows(BadCredentialsException.class, () -> passwords.change(owner.ownerUserId(),
                new ChangePasswordRequest("incorrect", "another long password")));
        assertEquals(1, sessions.countByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(owner.ownerUserId(), OffsetDateTime.now()));
        passwords.change(owner.ownerUserId(), new ChangePasswordRequest(PASSWORD, "another long password"));
        assertEquals(0, sessions.countByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(owner.ownerUserId(), OffsetDateTime.now()));
        assertThrows(BadCredentialsException.class, () -> authentication.refresh(new RefreshTokenRequest(token.refreshToken())));
        assertTrue(encoder.matches("another long password", users.findById(owner.ownerUserId()).orElseThrow().getPasswordHash()));
    }
    @Test void concurrentRefreshHasOnlyOneSuccessor() throws Exception {
        var owner = owner();
        String email = users.findById(owner.ownerUserId()).orElseThrow().getEmail();
        var token = authentication.login(new LoginRequest(email, PASSWORD));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> rotate = () -> {
                start.await();
                try { authentication.refresh(new RefreshTokenRequest(token.refreshToken())); return true; }
                catch (BadCredentialsException ex) { return false; }
            };
            var a = executor.submit(rotate);
            var b = executor.submit(rotate);
            start.countDown();
            assertNotEquals(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        }
        assertEquals(1, sessions.countByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(owner.ownerUserId(), OffsetDateTime.now()));
    }
    @Test void crossUserLogoutDoesNotRevokeOtherUsersSession() {
        var first = owner();
        var other = owner();
        String email = users.findById(first.ownerUserId()).orElseThrow().getEmail();
        var token = authentication.login(new LoginRequest(email, PASSWORD));
        assertThrows(BadCredentialsException.class, () -> authentication.logout(other.ownerUserId(), new RefreshTokenRequest(token.refreshToken())));
        assertNotNull(authentication.refresh(new RefreshTokenRequest(token.refreshToken())));
    }
    @Test void permissionsDenyCrossHotelAccess() {
        var first = owner();
        var other = owner();
        assertThrows(AccessDeniedException.class, () -> guard.requireHotelPermission(
                other.ownerUserId(), first.hotel().id(), PermissionCode.HOTEL_SETTINGS_VIEW));
    }
    @Test void postgresRejectsCrossHotelRoleAssignment() {
        var first = owner();
        var other = owner();
        UUID role = jdbc.queryForObject("select id from roles where hotel_id=? and code='OWNER'", UUID.class, first.hotel().id());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into membership_roles(id,membership_id,hotel_id,role_id) values (?,?,?,?)",
                UUID.randomUUID(), other.membershipId(), other.hotel().id(), role));
    }
}

