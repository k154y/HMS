package com.hotelmanagement.hms.report.web;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.model.Money;
import com.hotelmanagement.hms.payment.model.PaymentMethod;
import com.hotelmanagement.hms.audit.service.AuditService;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.web.bind.annotation.*;import org.springframework.transaction.annotation.Transactional;
import java.util.*;import java.time.*;import java.math.*;
@RestController @Transactional @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/expenses")
public class ExpenseController {
 private final JdbcTemplate db;private final OperationScope scope;private final AuditService audit;public ExpenseController(JdbcTemplate d,OperationScope s,AuditService a){db=d;scope=s;audit=a;}
 public record Expense(LocalDate date,String category,String description,BigDecimal amount,PaymentMethod method,UUID requestId){}
 @GetMapping public Object list(@PathVariable UUID hotel,@PathVariable UUID branch){scope.branch(hotel,branch,FINANCIAL_REPORT_VIEW);return db.queryForList("select * from expenses where hotel_id=? and branch_id=? order by expense_date desc,created_at desc limit 1000",hotel,branch);}
 @PostMapping public Object create(@PathVariable UUID hotel,@PathVariable UUID branch,@RequestBody Expense r){
  UUID actor=scope.branch(hotel,branch,CASHIER_RECONCILE);
  if(r.date()==null||r.requestId()==null||r.category()==null||r.category().isBlank()||r.category().length()>100||r.description()==null||r.description().isBlank()||r.description().length()>1000||r.method()==null||r.method()==PaymentMethod.CREDIT)throw new IllegalArgumentException("Invalid expense.");var amount=Money.positive(r.amount());
  var old=db.queryForList("select * from expenses where hotel_id=? and branch_id=? and request_id=?",hotel,branch,r.requestId());
  if(!old.isEmpty()){var e=old.getFirst();if(amount.compareTo((BigDecimal)e.get("amount"))!=0||!r.description().equals(e.get("description"))||!r.method().name().equals(e.get("method")))throw new IllegalStateException("Expense request reused.");return e;}
  UUID id=UUID.randomUUID();db.update("insert into expenses(id,hotel_id,branch_id,expense_date,category,description,amount,method,request_id,actor_id) values(?,?,?,?,?,?,?,?,?,?)",id,hotel,branch,r.date(),r.category(),r.description(),amount,r.method().name(),r.requestId(),actor);audit.record(hotel,branch,actor,"EXPENSE_RECORDED","EXPENSE",id);return Map.of("id",id);
 }
}
