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
  if(!old.isEmpty()){if(amount.compareTo((BigDecimal)old.getFirst().get("amount"))!=0)throw new com.hotelmanagement.hms.shared.web.BusinessRuleException("Request key reused.");return balance(h,b,q.customerId());}
  var f=folios.open(h,b,q.customerId(),actor);folios.post(h,b,f.getId(),amount,EntryKind.CHARGE,q.sourceId(),q.description(),actor);f.credit();
  db.update("insert into credit_ledger(id,hotel_id,branch_id,customer_id,kind,amount,source_id,description,actor_id) values(?,?,?,?,'CHARGE',?,?,?,?)",UUID.randomUUID(),h,b,q.customerId(),amount,q.sourceId(),q.description(),actor);
  audit.record(h,b,actor,"CREDIT_CHARGED","FOLIO",f.getId());return balance(h,b,q.customerId());
 }
 public void approveFolio(UUID h,UUID b,UUID id){
  UUID actor=scope.branch(h,b,CREDIT_APPROVE);var f=folios.lock(h,b,id);
  if(f.getStatus()==FolioStatus.CREDIT)return;
  var amount=folios.balance(h,b,id);if(amount.signum()<=0)throw new com.hotelmanagement.hms.shared.web.BusinessRuleException("No outstanding balance.");f.credit();
  db.update("insert into credit_ledger(id,hotel_id,branch_id,customer_id,kind,amount,source_id,description,actor_id) values(?,?,?,?,'CHARGE',?,?,?,?)",UUID.randomUUID(),h,b,f.getCustomerId(),amount,id,"Folio credit "+id,actor);
  audit.record(h,b,actor,"CREDIT_APPROVED","FOLIO",id);
 }
 public CreditResponse settle(UUID h,UUID b,CreditRequest q){
  UUID actor=scope.branch(h,b,CREDIT_MANAGE);
  scope.branch(h,b,CREDIT_VIEW);scope.branch(h,b,PAYMENT_RECORD);
  customer(h,q.customerId());var remaining=Money.positive(q.amount());
  if(q.sourceId()==null || q.description()==null || q.description().isBlank() || q.description().length()>1000)
   throw new IllegalArgumentException("Valid settlement request ID and description required.");
  var response=balance(h,b,q.customerId());
  int inserted=db.update("""
      insert into credit_settlement_commands(hotel_id,request_id,branch_id,customer_id,amount,description,response_balance,actor_id)
      values(?,?,?,?,?,?,?,?) on conflict(hotel_id,request_id) do nothing
      """,h,q.sourceId(),b,q.customerId(),remaining,q.description(),response.balance(),actor);
  var command=db.queryForMap("select * from credit_settlement_commands where hotel_id=? and request_id=?",h,q.sourceId());
  if(!b.equals(command.get("branch_id")) || !q.customerId().equals(command.get("customer_id"))
      || remaining.compareTo((BigDecimal)command.get("amount"))!=0 || !q.description().equals(command.get("description")))
   throw new BusinessRuleException("Settlement request key reused with different data.");
  if(inserted==0){
   var allocations=db.queryForList("select * from credit_settlement_allocations where hotel_id=? and request_id=? order by folio_id",h,q.sourceId());
   if(allocations.isEmpty())throw new BusinessRuleException("Settlement allocation history is incomplete.");
   for(var allocation:allocations){
    // Reuse the frozen child requests, including PaymentWorkflow's current
    // authorization checks. Its idempotency check precedes balance/FX checks.
    var approval=settlementPayment(h,b,(UUID)allocation.get("folio_id"),(UUID)allocation.get("payment_request_id"),(BigDecimal)allocation.get("amount"));
    if(!approval.get("id").equals(allocation.get("payment_approval_id")))throw new BusinessRuleException("Settlement approval history does not match.");
   }
   return new CreditResponse(q.customerId(),(BigDecimal)command.get("response_balance"));
  }
  if(hasLegacySettlement(h,q.sourceId()))throw new BusinessRuleException("LEGACY_SETTLEMENT_REVIEW_REQUIRED","Historical settlement requires review; its original allocation plan was not recorded.");
  // Lock the candidate folios before reading balances. Posting also locks folios.
  // The customer lock continues to serialize settlement commands for this customer.
  db.queryForList("select id from folios where hotel_id=? and branch_id=? and customer_id=? and status='CREDIT' order by id for update",h,b,q.customerId());
  var accounts=db.queryForList("select f.id,coalesce(sum(e.amount),0) balance from folios f left join folio_entries e on e.folio_id=f.id where f.hotel_id=? and f.branch_id=? and f.customer_id=? and f.status='CREDIT' group by f.id order by f.id",h,b,q.customerId());
  var total=accounts.stream().map(r->(BigDecimal)r.get("balance")).reduce(BigDecimal.ZERO,BigDecimal::add);
  if(remaining.compareTo(total)>0)throw new com.hotelmanagement.hms.shared.web.BusinessRuleException("Payment exceeds credit balance.");
  for(var row:accounts){
   BigDecimal amount=remaining.min((BigDecimal)row.get("balance"));if(amount.signum()<=0)continue;
   UUID id=(UUID)row.get("id");UUID key=UUID.nameUUIDFromBytes((q.sourceId()+":"+id).getBytes(java.nio.charset.StandardCharsets.UTF_8));
   var approval=settlementPayment(h,b,id,key,amount);
   db.update("insert into credit_settlement_allocations(hotel_id,branch_id,request_id,folio_id,amount,payment_request_id,payment_approval_id) values(?,?,?,?,?,?,?)",h,b,q.sourceId(),id,amount,key,approval.get("id"));
   remaining=remaining.subtract(amount);
  }
  if(remaining.signum()!=0)throw new BusinessRuleException("Settlement could not be fully allocated.");
  audit.record(h,b,actor,"CREDIT_SETTLEMENT_REQUESTED","CREDIT_SETTLEMENT",q.sourceId());
  return response;
 }
 private Map<String,Object> settlementPayment(UUID h,UUID b,UUID folio,UUID key,BigDecimal amount){
  return payments.request(h,b,new PaymentWorkflow.Request(folio,key,List.of(new PaymentWorkflow.Part(PaymentMethod.CASH,amount))));
 }
 private boolean hasLegacySettlement(UUID hotel,UUID request){
  // Older approvals stored only UUID.nameUUIDFromBytes(parent + ':' + folio).
  // Reproduce UUID v3's version/variant bits in PostgreSQL, checking the entire
  // tenant so changed-customer/branch retries cannot evade the historical guard.
  // A matching child proves prior use, but cannot recover the original parent
  // amount, description, allocation plan or response. Fail closed without edits.
  return Boolean.TRUE.equals(db.queryForObject("""
      select exists(
       select 1 from payment_approvals p
       cross join lateral (select md5(?::text || ':' || p.folio_id::text) digest) k
       where p.hotel_id=? and p.request_id=(
        substr(digest,1,12)||'3'||substr(digest,14,3)||
        substr('89ab',((strpos('0123456789abcdef',substr(digest,17,1))-1)%4)+1,1)||substr(digest,18)
       )::uuid
      )
      """,Boolean.class,request,hotel));
 }
 public CreditResponse balance(UUID h,UUID b,UUID c){scope.branch(h,b,CREDIT_VIEW);if(db.queryForList("select id from customers where id=? and hotel_id=?",c,h).isEmpty())throw ApiException.notFound();return new CreditResponse(c,db.queryForObject("select coalesce(sum(amount),0) from credit_ledger where hotel_id=? and branch_id=? and customer_id=?",BigDecimal.class,h,b,c));}
 public List<Map<String,Object>> list(UUID h,UUID b){scope.branch(h,b,CREDIT_VIEW);return db.queryForList("select c.id,c.name,coalesce(sum(e.amount),0) balance from customers c join credit_ledger e on e.customer_id=c.id where e.hotel_id=? and e.branch_id=? group by c.id order by c.name",h,b);}
 public List<Map<String,Object>> history(UUID h,UUID b,UUID c){balance(h,b,c);return db.queryForList("select * from credit_ledger where hotel_id=? and branch_id=? and customer_id=? order by entry_date desc limit 1000",h,b,c);}
}
