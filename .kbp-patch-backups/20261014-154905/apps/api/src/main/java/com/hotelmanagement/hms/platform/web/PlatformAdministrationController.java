package com.hotelmanagement.hms.platform.web;

import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import com.hotelmanagement.hms.platform.onboarding.dto.OwnerSignupRequest;
import com.hotelmanagement.hms.platform.onboarding.service.OwnerOnboardingService;
import com.hotelmanagement.hms.platform.service.HotelAdministrationService;
import com.hotelmanagement.hms.platform.dto.CreateBranchRequest;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/platform/hotels")
public class PlatformAdministrationController {
 private final JdbcTemplate jdbc;
 private final AuthenticatedUserContext context;
 private final OwnerOnboardingService onboarding;
 private final HotelAdministrationService hotels;
 public PlatformAdministrationController(JdbcTemplate jdbc, AuthenticatedUserContext context, OwnerOnboardingService onboarding, HotelAdministrationService hotels) {
  this.jdbc=jdbc; this.context=context; this.onboarding=onboarding; this.hotels=hotels;
 }
 private UUID requireAdmin() {
  UUID actor=context.requireCurrentUserId();
  if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from platform_administrators where user_id=?)",Boolean.class,actor))) throw new AccessDeniedException("Platform administrator required.");
  return actor;
 }
 @GetMapping public List<Map<String,Object>> list() {
  requireAdmin();
  return jdbc.queryForList("select id,code,display_name as name,status,trial_ends_at from hotels order by created_at desc limit 200");
 }
 @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @Transactional
 public Map<String,Object> create(@Valid @RequestBody OwnerSignupRequest request) {
  UUID actor=requireAdmin();
  var result=onboarding.signup(request);
  var branch=hotels.createBranch(result.ownerUserId(),result.hotel().id(),new CreateBranchRequest("MAIN","Main branch",null,null,null,null));
  jdbc.update("insert into platform_audit_events(id,actor_id,hotel_id,action) values(?,?,?,?)",UUID.randomUUID(),actor,result.hotel().id(),"HOTEL_OWNER_CREATED");
  return Map.of("ownerUserId",result.ownerUserId(),"hotel",result.hotel(),"branch",branch);
 }
}
