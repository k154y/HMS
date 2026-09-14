package com.hotelmanagement.hms.folio.service;
import com.hotelmanagement.hms.folio.model.*;
import com.hotelmanagement.hms.folio.dto.*;
import com.hotelmanagement.hms.folio.repository.*;
import com.hotelmanagement.hms.customer.repository.CustomerRepository;
import com.hotelmanagement.hms.platform.repository.HotelRepository;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.model.Money;
import com.hotelmanagement.hms.shared.web.*;
import com.hotelmanagement.hms.audit.service.AuditService;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;
import java.math.BigDecimal;
@Service @Transactional
public class FolioService {
 private final FolioRepository folios; private final FolioEntryRepository entries; private final CustomerRepository customers;
 private final HotelRepository hotels; private final OperationScope scope; private final AuditService audit;
 private final org.springframework.jdbc.core.JdbcTemplate db;
 public FolioService(FolioRepository folios,FolioEntryRepository entries,CustomerRepository customers,HotelRepository hotels,OperationScope scope,AuditService audit,org.springframework.jdbc.core.JdbcTemplate db) {
  this.folios=folios;this.entries=entries;this.customers=customers;this.hotels=hotels;this.scope=scope;this.audit=audit;this.db=db;
 }
 public FolioResponse create(UUID hotel,UUID branch,UUID customer) {
  UUID actor=scope.branch(hotel,branch,FOLIO_MANAGE);var e=open(hotel,branch,customer,actor);return response(e);
 }
 @Transactional(propagation=Propagation.MANDATORY) public Folio forCustomer(UUID hotel,UUID branch,UUID customer,UUID actor){
  customers.lock(customer,hotel).filter(c->c.getActive()).orElseThrow(ApiException::notFound);
  var existing=db.queryForList("select f.id from folios f where f.hotel_id=? and f.branch_id=? and f.customer_id=? and f.status='OPEN' order by case when exists(select 1 from reservations r where r.folio_id=f.id and r.status='CHECKED_IN') then 0 else 1 end,f.created_at,f.id limit 1",hotel,branch,customer);
  if(!existing.isEmpty())return lock(hotel,branch,(UUID)existing.getFirst().get("id"));
  return open(hotel,branch,customer,actor);
 }
 @Transactional(propagation=Propagation.MANDATORY) public Folio open(UUID hotel,UUID branch,UUID customer,UUID actor) {
  customers.findByIdAndHotelId(customer,hotel).filter(c->c.getActive()).orElseThrow(ApiException::notFound);
  var e=folios.saveAndFlush(Folio.create(hotel,branch,customer,hotels.findById(hotel).orElseThrow(ApiException::notFound).getCurrencyCode(),FolioStatus.OPEN));
  audit.record(hotel,branch,actor,"FOLIO_OPENED","FOLIO",e.getId());return e;
 }
 @Transactional(readOnly=true) public FolioResponse get(UUID hotel,UUID branch,UUID id) {
  scope.branch(hotel,branch,FOLIO_VIEW);return response(folios.findByIdAndHotelIdAndBranchId(id,hotel,branch).orElseThrow(ApiException::notFound));
 }
 @Transactional(readOnly=true) public PageResponse<FolioEntryResponse> entries(UUID hotel,UUID branch,UUID id,int page,int size) {
  scope.branch(hotel,branch,FOLIO_VIEW);folios.findByIdAndHotelIdAndBranchId(id,hotel,branch).orElseThrow(ApiException::notFound);
  return PageResponse.from(entries.findByHotelIdAndBranchIdAndFolioId(hotel,branch,id,scope.page(page,size)).map(FolioEntryResponse::from));
 }
 public FolioEntryResponse charge(UUID hotel,UUID branch,UUID id,BigDecimal amount,String memo,UUID requestId) {
  UUID actor=scope.branch(hotel,branch,FOLIO_MANAGE);return FolioEntryResponse.from(post(hotel,branch,id,Money.positive(amount),EntryKind.CHARGE,requestId,memo,actor));
 }
 @Transactional(propagation=Propagation.MANDATORY) public FolioEntry post(UUID hotel,UUID branch,UUID id,BigDecimal amount,EntryKind kind,UUID source,String memo,UUID actor) {
  var f=lock(hotel,branch,id);
  if(kind==EntryKind.REFUND)f.reopenForRefund();
  if(f.getStatus()==FolioStatus.CLOSED || (f.getStatus()==FolioStatus.CREDIT && kind!=EntryKind.PAYMENT && kind!=EntryKind.REFUND))
   throw new IllegalStateException("Folio does not accept this entry.");
  if(amount==null || Money.amount(amount).signum()==0 || source==null || memo==null || memo.isBlank() || memo.length()>1000) throw new IllegalArgumentException("Invalid entry.");
  var e=entries.saveAndFlush(FolioEntry.create(hotel,branch,id,Money.amount(amount),kind,source,memo,actor));
  if(f.getStatus()==FolioStatus.CREDIT){
   db.update("insert into credit_ledger(id,hotel_id,branch_id,customer_id,kind,amount,source_id,description,actor_id) values(?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),hotel,branch,f.getCustomerId(),kind==EntryKind.PAYMENT?"PAYMENT":"ADJUSTMENT",Money.amount(amount),e.getId(),memo,actor);
  }
  audit.record(hotel,branch,actor,"FOLIO_"+kind,"FOLIO_ENTRY",e.getId());return e;
 }
 public FolioEntryResponse reverse(UUID hotel,UUID branch,UUID id,UUID entry,String reason) {
  UUID actor=scope.branch(hotel,branch,FOLIO_MANAGE);lock(hotel,branch,id);
  var original=entries.findByIdAndHotelIdAndBranchId(entry,hotel,branch).filter(e->e.getFolioId().equals(id)).orElseThrow(ApiException::notFound);
  if(original.getKind()!=EntryKind.CHARGE) throw new IllegalStateException("Use the originating module to reverse this entry.");
  return FolioEntryResponse.from(post(hotel,branch,id,original.getAmount().negate(),EntryKind.REVERSAL,entry,reason,actor));
 }
 public void transfer(UUID hotel,UUID branch,UUID from,UUID to,BigDecimal amount,String reason,UUID requestId) {
  UUID actor=scope.branch(hotel,branch,FOLIO_TRANSFER);if(from.equals(to))throw new IllegalArgumentException("Same folio.");
  // Stable lock order prevents opposite transfers deadlocking.
  if(from.compareTo(to)<0) { lock(hotel,branch,from);lock(hotel,branch,to); } else { lock(hotel,branch,to);lock(hotel,branch,from); }
  amount=Money.positive(amount);if(balance(hotel,branch,from).compareTo(amount)<0)throw new IllegalStateException("Transfer exceeds outstanding balance.");
  post(hotel,branch,from,amount.negate(),EntryKind.TRANSFER_OUT,requestId,reason,actor);
  post(hotel,branch,to,amount,EntryKind.TRANSFER_IN,requestId,reason,actor);
 }
 @Transactional(propagation=Propagation.MANDATORY) public Folio lock(UUID hotel,UUID branch,UUID id) { return folios.lock(id,hotel,branch).orElseThrow(ApiException::notFound); }
 @Transactional(readOnly=true) public BigDecimal balance(UUID hotel,UUID branch,UUID id) { return entries.balance(hotel,branch,id); }
 @Transactional(propagation=Propagation.MANDATORY) public void closeSettled(UUID hotel,UUID branch,UUID id) {
  var f=lock(hotel,branch,id);if(f.getStatus()==FolioStatus.CREDIT)return;
  if(balance(hotel,branch,id).signum()!=0)throw new ApiException(409,"FOLIO_UNSETTLED","Folio must be settled before checkout.");f.close();
 }
 @Transactional(readOnly=true) public List<Map<String,Object>> list(UUID hotel,UUID branch){
  scope.branch(hotel,branch,FOLIO_VIEW);
  return db.queryForList("select f.*,c.name customer_name,coalesce(sum(e.amount),0) balance from folios f join customers c on c.id=f.customer_id left join folio_entries e on e.folio_id=f.id where f.hotel_id=? and f.branch_id=? group by f.id,c.name order by f.created_at desc limit 100",hotel,branch);
 }
 private FolioResponse response(Folio f) { return FolioResponse.from(f,balance(f.getHotelId(),f.getBranchId(),f.getId())); }
}
