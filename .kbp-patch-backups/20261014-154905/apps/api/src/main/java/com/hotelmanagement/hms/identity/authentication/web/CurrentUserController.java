package com.hotelmanagement.hms.identity.authentication.web;

import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/auth/me")
public class CurrentUserController {
 private final JdbcTemplate jdbc; private final AuthenticatedUserContext context;
 public CurrentUserController(JdbcTemplate jdbc,AuthenticatedUserContext context){this.jdbc=jdbc;this.context=context;}
 @GetMapping public Map<String,Object> me(){
  UUID actor=context.requireCurrentUserId();
  Map<String,Object> user=new LinkedHashMap<>(jdbc.queryForMap("select id,email,full_name as name,preferred_language as language from users where id=?",actor));
  boolean admin=Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from platform_administrators where user_id=?)",Boolean.class,actor));
  user.put("role",admin?"SUPER_ADMIN":null);
  var memberships=jdbc.queryForList("""
   select m.id as membership_id,m.hotel_id,h.display_name,h.status,h.trial_ends_at,
   (select b.id from branches b where b.hotel_id=m.hotel_id and b.is_active=true
    and (m.all_branches or exists(select 1 from membership_branch_access a where a.membership_id=m.id and a.branch_id=b.id)) order by b.created_at limit 1) as branch_id
   from hotel_memberships m join hotels h on h.id=m.hotel_id
   where m.user_id=? and m.status='ACTIVE' order by m.created_at
   """,actor);
  user.put("memberships",memberships);
  if(!admin && !memberships.isEmpty()){
   var m=memberships.getFirst();
   var roles=jdbc.queryForList("select r.code from membership_roles mr join roles r on r.id=mr.role_id where mr.membership_id=? and r.active=true order by case r.code when 'OWNER' then 0 when 'MANAGER' then 1 else 2 end,r.code",String.class,m.get("membership_id"));
   user.put("roles",roles);user.put("role",roles.isEmpty()?null:roles.getFirst());
   user.put("permissions",jdbc.queryForList("select distinct p.code from membership_roles mr join roles r on r.id=mr.role_id join role_permissions rp on rp.role_id=r.id join permissions p on p.id=rp.permission_id where mr.membership_id=? and r.active=true order by p.code",String.class,m.get("membership_id")));
   user.put("hotelId",m.get("hotel_id"));user.put("branchId",m.get("branch_id"));user.put("hotelName",m.get("display_name"));user.put("subscriptionStatus",m.get("status"));user.put("trialEndDate",m.get("trial_ends_at"));
  }
  return user;
 }
}
