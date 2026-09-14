package com.hotelmanagement.hms.report.web;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.identity.authorization.model.PermissionCode;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;import java.util.*;import java.time.*;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/dashboard")
public class DashboardController {
 private final JdbcTemplate db;private final OperationScope scope;public DashboardController(JdbcTemplate d,OperationScope s){db=d;scope=s;}
 private boolean allowed(UUID h,UUID b,PermissionCode p){try{scope.branch(h,b,p);return true;}catch(AccessDeniedException denied){return false;}}
 @GetMapping public Object dashboard(@PathVariable UUID hotel,@PathVariable UUID branch){
  scope.branch(hotel,branch,BRANCH_VIEW);String zone=db.queryForObject("select timezone from hotels where id=?",String.class,hotel);var date=LocalDate.now(ZoneId.of(zone));var cards=new ArrayList<Map<String,Object>>();
  if(allowed(hotel,branch,ROOM_VIEW)){cards.add(Map.of("label","Rooms","value",db.queryForObject("select count(*) from rooms where hotel_id=? and branch_id=? and active",Long.class,hotel,branch),"href","/rooms"));cards.add(Map.of("label","Rooms ready","value",db.queryForObject("select count(*) from rooms where hotel_id=? and branch_id=? and active and operational='AVAILABLE' and housekeeping in ('CLEAN','INSPECTED')",Long.class,hotel,branch),"href","/housekeeping"));}
  if(allowed(hotel,branch,RESERVATION_VIEW)){cards.add(Map.of("label","Expected arrivals","value",db.queryForObject("select count(*) from reservations where hotel_id=? and branch_id=? and check_in=? and status in ('PENDING','CONFIRMED')",Long.class,hotel,branch,date),"href","/checkin"));cards.add(Map.of("label","Expected departures","value",db.queryForObject("select count(*) from reservations where hotel_id=? and branch_id=? and check_out=? and status='CHECKED_IN'",Long.class,hotel,branch,date),"href","/checkout"));}
  if(allowed(hotel,branch,ORDER_VIEW))cards.add(Map.of("label","Open orders","value",db.queryForObject("select count(*) from orders where hotel_id=? and branch_id=? and status in ('SENT','PREPARING','READY')",Long.class,hotel,branch),"href","/pos"));
  if(allowed(hotel,branch,FOLIO_VIEW))cards.add(Map.of("label","Outstanding balance","value",db.queryForObject("select coalesce(sum(amount),0) from folio_entries where hotel_id=? and branch_id=?",java.math.BigDecimal.class,hotel,branch),"href","/folios"));
  if(allowed(hotel,branch,PAYMENT_VIEW))cards.add(Map.of("label","Pending payment approvals","value",db.queryForObject("select count(*) from payment_approvals where hotel_id=? and branch_id=? and status='PENDING'",Long.class,hotel,branch),"href","/cashier"));
  return Map.of("date",date,"cards",cards);
 }
}
