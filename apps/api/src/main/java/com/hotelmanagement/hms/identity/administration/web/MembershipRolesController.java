package com.hotelmanagement.hms.identity.administration.web;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.ApiException;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/memberships")
public class MembershipRolesController {
 private final OperationScope scope;private final JdbcTemplate db;public MembershipRolesController(OperationScope s,JdbcTemplate d){scope=s;db=d;}
 @GetMapping("/{id}/roles") public Object roles(@PathVariable UUID hotel,@PathVariable UUID id){scope.hotel(hotel,USER_VIEW);if(db.queryForList("select id from hotel_memberships where id=? and hotel_id=?",id,hotel).isEmpty())throw ApiException.notFound();return db.queryForList("select r.id,r.code,r.name from membership_roles mr join roles r on r.id=mr.role_id where mr.membership_id=? and r.hotel_id=? order by r.code",id,hotel);}
}
