package com.hotelmanagement.hms.credit.service;
import com.hotelmanagement.hms.credit.dto.*;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.model.Money;
import com.hotelmanagement.hms.shared.web.*;
import com.hotelmanagement.hms.folio.service.FolioService;
import com.hotelmanagement.hms.folio.model.*;
import com.hotelmanagement.hms.payment.service.PaymentWorkflow;
import com.hotelmanagement.hms.payment.model.PaymentMethod;
import com.hotelmanagement.hms.audit.service.AuditService;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;import java.math.*;
@Service @Transactional
public class CreditService {
 private final JdbcTemplate db;private final OperationScope scope;private final FolioService folios;private final PaymentWorkflow payments;private final AuditService audit;
 public CreditService(JdbcTemplate d,OperationScope s,FolioService f,PaymentWorkflow p,AuditService a){db=d;scope=s;folios=f;payments=p;audit=a;}
 private void customer(UUID h,UUID c){if(db.queryForList("select id from customers where id=? and hotel_id=? for update",c,h).isEmpty())throw ApiException.notFound();}
 public CreditResponse charge(UUID h,UUID b,CreditRequest q){
  UUID actor=scope.branch(h,b,CREDIT_MANAGE);customer(h,q.customerId());var amount=Money.positive(q.amount());
  if(q.sourceId()==null)throw new IllegalArgumentException("Request ID required.");
  var old=db.queryForList("select amount from credit_ledger where hotel_id=? and branch_id=? and customer_id=? and kind='CHARGE' and source_id=?",h,b,q.customerId(),q.sourceId());
  if(!old.isEmpty()){if(amount.compareTo((BigDecimal)old.getFirst().get("amount"))!=0)throw new IllegalStateException("Request key reused.");return balance(h,b,q.customerId());}
  var f=folios.open(h,b,q.customerId(),actor);folios.post(h,b,f.getId(),amount,EntryKind.CHARGE,q.sourceId(),q.description(),actor);f.credit();
  db.update("insert into credit_ledger(id,hotel_id,branch_id,customer_id,kind,amount,source_id,description,actor_id) values(?,?,?,?,'CHARGE',?,?,?,?)",UUID.randomUUID(),h,b,q.customerId(),amount,q.sourceId(),q.description(),actor);
  audit.record(h,b,actor,"CREDIT_CHARGED","FOLIO",f.getId());return balance(h,b,q.customerId());
 }
 public void approveFolio(UUID h,UUID b,UUID id){
  UUID actor=scope.branch(h,b,CREDIT_APPROVE);var f=folios.lock(h,b,id);
  if(f.getStatus()==FolioStatus.CREDIT)return;
  var amount=folios.balance(h,b,id);if(amount.signum()<=0)throw new IllegalStateException("No outstanding balance.");f.credit();
  db.update("insert into credit_ledger(id,hotel_id,branch_id,customer_id,kind,amount,source_id,description,actor_id) values(?,?,?,?,'CHARGE',?,?,?,?)",UUID.randomUUID(),h,b,f.getCustomerId(),amount,id,"Folio credit "+id,actor);
  audit.record(h,b,actor,"CREDIT_APPROVED","FOLIO",id);
 }
 public CreditResponse settle(UUID h,UUID b,CreditRequest q){
  scope.branch(h,b,CREDIT_MANAGE);customer(h,q.customerId());var remaining=Money.positive(q.amount());
  if(q.sourceId()==null)throw new IllegalArgumentException("Request ID required.");
  var accounts=db.queryForList("select f.id,coalesce(sum(e.amount),0) balance from folios f left join folio_entries e on e.folio_id=f.id where f.hotel_id=? and f.branch_id=? and f.customer_id=? and f.status='CREDIT' group by f.id order by f.id",h,b,q.customerId());
  var total=accounts.stream().map(r->(BigDecimal)r.get("balance")).reduce(BigDecimal.ZERO,BigDecimal::add);
  if(remaining.compareTo(total)>0)throw new IllegalStateException("Payment exceeds credit balance.");
  for(var row:accounts){BigDecimal amount=remaining.min((BigDecimal)row.get("balance"));if(amount.signum()<=0)continue;UUID id=(UUID)row.get("id");UUID key=UUID.nameUUIDFromBytes((q.sourceId()+":"+id).getBytes(java.nio.charset.StandardCharsets.UTF_8));payments.request(h,b,new PaymentWorkflow.Request(id,key,List.of(new PaymentWorkflow.Part(PaymentMethod.CASH,amount))));remaining=remaining.subtract(amount);}
  return balance(h,b,q.customerId());
 }
 public CreditResponse balance(UUID h,UUID b,UUID c){scope.branch(h,b,CREDIT_VIEW);if(db.queryForList("select id from customers where id=? and hotel_id=?",c,h).isEmpty())throw ApiException.notFound();return new CreditResponse(c,db.queryForObject("select coalesce(sum(amount),0) from credit_ledger where hotel_id=? and branch_id=? and customer_id=?",BigDecimal.class,h,b,c));}
 public List<Map<String,Object>> list(UUID h,UUID b){scope.branch(h,b,CREDIT_VIEW);return db.queryForList("select c.id,c.name,coalesce(sum(e.amount),0) balance from customers c join credit_ledger e on e.customer_id=c.id where e.hotel_id=? and e.branch_id=? group by c.id order by c.name",h,b);}
 public List<Map<String,Object>> history(UUID h,UUID b,UUID c){balance(h,b,c);return db.queryForList("select * from credit_ledger where hotel_id=? and branch_id=? and customer_id=? order by entry_date desc limit 1000",h,b,c);}
}
