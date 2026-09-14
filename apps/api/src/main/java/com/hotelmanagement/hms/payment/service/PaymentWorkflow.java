package com.hotelmanagement.hms.payment.service;
import com.hotelmanagement.hms.payment.dto.PaymentRequest;
import com.hotelmanagement.hms.payment.model.PaymentMethod;
import com.hotelmanagement.hms.folio.service.FolioService;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.ApiException;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;import tools.jackson.databind.json.JsonMapper;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;import java.math.*;
@Service @Transactional
public class PaymentWorkflow {
 private final JdbcTemplate db;private final OperationScope scope;private final FolioService folios;private final PaymentService payments;private final JsonMapper json;private final com.hotelmanagement.hms.audit.service.AuditService audit;
 public PaymentWorkflow(JdbcTemplate d,OperationScope s,FolioService f,PaymentService p,JsonMapper j,com.hotelmanagement.hms.audit.service.AuditService a){db=d;scope=s;folios=f;payments=p;json=j;audit=a;}
 public record Part(PaymentMethod method,BigDecimal amount){}
 public record Request(UUID folioId,UUID requestId,List<Part> parts,String collectionScope){
  public Request(UUID folioId,UUID requestId,List<Part> parts){this(folioId,requestId,parts,null);}
 }
 public Map<String,Object> requestSingle(UUID h,UUID b,PaymentRequest r){
  scope.branch(h,b,PAYMENT_RECORD);
  var folio=folios.lock(h,b,r.folioId());
  if(!folio.getCurrency().equals(r.currency())||r.fxRate().compareTo(BigDecimal.ONE)!=0)throw new IllegalArgumentException("Payment must use folio currency.");
  UUID key=UUID.nameUUIDFromBytes(r.idempotencyKey().getBytes(java.nio.charset.StandardCharsets.UTF_8));
  return request(h,b,new Request(r.folioId(),key,List.of(new Part(r.method(),r.amount()))));
 }
 private void validate(Request r){
  if(r==null||r.folioId()==null||r.requestId()==null||r.parts()==null||r.parts().isEmpty()||r.parts().size()>8)throw new IllegalArgumentException("Invalid payment.");
  for(var p:r.parts())if(p==null||p.method()==null||p.method()==PaymentMethod.CREDIT||p.amount()==null||p.amount().signum()<=0||p.amount().scale()>4)throw new IllegalArgumentException("Invalid payment part.");
 }
 public Map<String,Object> request(UUID h,UUID b,Request r){
  validate(r);UUID actor=scope.branch(h,b,PAYMENT_RECORD);folios.lock(h,b,r.folioId());
  boolean room=Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from folio_entries where folio_id=? and kind='ACCOMMODATION')",Boolean.class,r.folioId()));
  String collection=r.collectionScope()==null?(room?"ROOM":"FOOD"):r.collectionScope();
  if(!Set.of("ROOM","FOOD").contains(collection))throw new IllegalArgumentException("Invalid collection scope.");
  if(collection.equals("ROOM")&&!hasRole(actor,h,Set.of("OWNER","MANAGER","RECEPTIONIST")))throw new AccessDeniedException("Room payments require reception.");
  r=new Request(r.folioId(),r.requestId(),r.parts(),collection);
  var old=db.queryForList("select * from payment_approvals where hotel_id=? and branch_id=? and request_id=?",h,b,r.requestId());
  String payload=json.writeValueAsString(r);
  if(!old.isEmpty()){if(!old.getFirst().get("payload").equals(payload))throw new IllegalStateException("Request key reused.");return old.getFirst();}
  var total=r.parts().stream().map(Part::amount).reduce(BigDecimal.ZERO,BigDecimal::add);
  if(total.compareTo(folios.balance(h,b,r.folioId()))>0)throw new IllegalStateException("Payment exceeds balance.");
  checkCollection(r,total);
  UUID id=UUID.randomUUID();db.update("insert into payment_approvals(id,hotel_id,branch_id,folio_id,request_id,requested_by,payload,status) values(?,?,?,?,?,?,?,'PENDING')",id,h,b,r.folioId(),r.requestId(),actor,payload);
  return Map.of("id",id,"status","PENDING");
 }
 public Map<String,Object> approve(UUID h,UUID b,UUID id,boolean approved){return approve(h,b,id,approved,null);}
 public Map<String,Object> approve(UUID h,UUID b,UUID id,boolean approved,String reason){
  UUID actor=scope.branch(h,b,PAYMENT_VIEW);
  if(!hasRole(actor,h,Set.of("OWNER","MANAGER","ACCOUNTANT","CASHIER","SUPERVISOR")))throw new AccessDeniedException("Payment approval not permitted.");
  var rows=db.queryForList("select * from payment_approvals where id=? and hotel_id=? and branch_id=? for update",id,h,b);
  if(rows.isEmpty())throw ApiException.notFound();var row=rows.getFirst();
  if(!row.get("status").equals("PENDING"))throw new IllegalStateException("Request already decided.");
  boolean self=row.get("requested_by").equals(actor);
  if(self&&!hasRole(actor,h,Set.of("OWNER")))throw new AccessDeniedException("A different user must approve payment.");
  if(reason!=null&&reason.length()>1000)throw new IllegalArgumentException("Reason is too long.");
  if(self&&(reason==null||reason.isBlank()))throw new IllegalStateException("Owner approval of their own payment requires a reason.");
  if(approved){if(db.queryForList("select id from cashier_shifts where hotel_id=? and branch_id=? and cashier_user_id=? and status='OPEN' for update",h,b,actor).isEmpty())throw new ApiException(409,"SHIFT_REQUIRED","Open your cashier shift before approving a payment.");Request r=json.readValue((String)row.get("payload"),Request.class);var f=folios.lock(h,b,r.folioId());
   checkCollection(r,r.parts().stream().map(Part::amount).reduce(BigDecimal.ZERO,BigDecimal::add));
   int index=0;for(var part:r.parts()){
    var payment=payments.create(h,b,new PaymentRequest(r.folioId(),part.method(),f.getCurrency(),part.amount(),BigDecimal.ONE,null,id+"-"+(index++)));
    db.update("update payments set collection_scope=? where id=?",r.collectionScope()==null?"FOOD":r.collectionScope(),payment.id());
   }
  }
  String status=approved?"APPROVED":"REJECTED";db.update("update payment_approvals set status=?,approved_by=?,decision_reason=?,decided_at=current_timestamp where id=?",status,actor,reason,id);
  audit.detailed(h,b,actor,self?"OWNER_PAYMENT_DECISION":"PAYMENT_DECISION","PAYMENT_APPROVAL",id,json.writeValueAsString(Map.of("status","PENDING")),json.writeValueAsString(Map.of("status",status,"ownerOverride",self)),reason,status);
  return Map.of("id",id,"status",status);
 }
 public List<Map<String,Object>> list(UUID h,UUID b){scope.branch(h,b,PAYMENT_VIEW);return db.queryForList("select a.id,a.folio_id,a.requested_by,a.approved_by,a.payload,a.status,a.created_at,a.decision_reason,a.decided_at,c.name customer_name,f.currency from payment_approvals a join folios f on f.id=a.folio_id join customers c on c.id=f.customer_id where a.hotel_id=? and a.branch_id=? order by a.created_at desc limit 100",h,b);}
 private void checkCollection(Request r,BigDecimal total){
  String collection=r.collectionScope()==null?"FOOD":r.collectionScope();
  BigDecimal charges=db.queryForObject("select coalesce(sum(amount),0) from folio_entries where folio_id=? and "+(collection.equals("ROOM")?"kind='ACCOMMODATION'":"kind not in ('ACCOMMODATION','PAYMENT','REFUND')"),BigDecimal.class,r.folioId());
  BigDecimal paid=db.queryForObject("select coalesce(sum(base_amount),0) from payments where folio_id=? and collection_scope=? and status='POSTED'",BigDecimal.class,r.folioId(),collection);
  if(total.compareTo(charges.subtract(paid))>0)throw new IllegalStateException("Payment exceeds the selected charge balance.");
 }
 private boolean hasRole(UUID actor,UUID h,Set<String> allowed){return db.queryForList("select r.code from hotel_memberships m join membership_roles mr on mr.membership_id=m.id join roles r on r.id=mr.role_id where m.user_id=? and m.hotel_id=? and m.status='ACTIVE' and r.active=true",String.class,actor,h).stream().anyMatch(allowed::contains);}
}



