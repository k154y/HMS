package com.hotelmanagement.hms;

import com.hotelmanagement.hms.credit.dto.CreditRequest;
import com.hotelmanagement.hms.credit.service.CreditService;
import com.hotelmanagement.hms.customer.dto.CustomerRequest;
import com.hotelmanagement.hms.customer.model.CustomerKind;
import com.hotelmanagement.hms.customer.service.CustomerService;
import com.hotelmanagement.hms.folio.service.FolioService;
import com.hotelmanagement.hms.inventory.model.MovementKind;
import com.hotelmanagement.hms.inventory.service.InventoryService;
import com.hotelmanagement.hms.payment.dto.CashierShiftRequest;
import com.hotelmanagement.hms.payment.service.*;
import com.hotelmanagement.hms.platform.dto.*;
import com.hotelmanagement.hms.platform.onboarding.dto.OwnerSignupRequest;
import com.hotelmanagement.hms.platform.onboarding.service.OwnerOnboardingService;
import com.hotelmanagement.hms.platform.service.HotelAdministrationService;
import com.hotelmanagement.hms.product.dto.ProductRequest;
import com.hotelmanagement.hms.product.model.Destination;
import com.hotelmanagement.hms.product.service.ProductService;
import com.hotelmanagement.hms.shared.web.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL transactions, including independent connections for races. */
@SpringBootTest(properties={"hms.security.jwt.secret=financial-integration-only-secret-at-least-32-bytes","logging.level.root=WARN","logging.level.com.hotelmanagement.hms=WARN","management.otlp.metrics.export.enabled=false"})
class CommandConsistencyTest {
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){IntegrationServices.configure(r);}
 @Autowired OwnerOnboardingService onboarding;
 @Autowired HotelAdministrationService hotels;
 @Autowired ProductService products;
 @Autowired InventoryService inventory;
 @Autowired CreditService credit;
 @Autowired CustomerService customers;
 @Autowired FolioService folios;
 @Autowired PaymentWorkflow payments;
 @Autowired CashierShiftService shifts;
 @Autowired JdbcTemplate db;
 @Autowired PlatformTransactionManager transactions;
 record Tenant(UUID user,UUID hotel,UUID branch){}
 Tenant tenant(){
  String code=UUID.randomUUID().toString();
  var owner=onboarding.signup(new OwnerSignupRequest(code+"@example.test","a secure hotel passphrase","Owner",null,"en",new CreateHotelRequest(code,"Hotel","Hotel",null,null,null,null,"RWF","Africa/Kigali","en")));
  UUID branch=hotels.createBranch(owner.ownerUserId(),owner.hotel().id(),new CreateBranchRequest("MAIN","Main",null,null,null,null)).id();
  var t=new Tenant(owner.ownerUserId(),owner.hotel().id(),branch);as(t);return t;
 }
 void as(Tenant t){SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg","HS256").subject(t.user().toString()).build(),List.of()));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 UUID branch(Tenant t){return hotels.createBranch(t.user(),t.hotel(),new CreateBranchRequest(UUID.randomUUID().toString(),"Other",null,null,null,null)).id();}
 UUID product(Tenant t){return products.create(t.hotel(),new ProductRequest(UUID.randomUUID().toString(),"Product","Goods","UNIT","UNIT","UNIT",BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ZERO,true,true,true,true,BigDecimal.ZERO,Destination.SERVICE)).id();}
 void opening(Tenant t,UUID p){inventory.adjust(t.hotel(),t.branch(),p,new BigDecimal("10"),MovementKind.OPENING,UUID.randomUUID(),"Opening");}
 void transfer(Tenant t,UUID target,UUID p,String amount,UUID key){inventory.transfer(t.hotel(),t.branch(),target,p,new BigDecimal(amount),key,"Transfer");}
 void stock(Tenant t,UUID b,UUID p,String expected){assertEquals(0,new BigDecimal(expected).compareTo(inventory.stock(t.hotel(),b,p).quantity()));}
 long movements(Tenant t,UUID key){return db.queryForObject("select count(*) from stock_movements where hotel_id=? and source_id=?",Long.class,t.hotel(),key);}
 UUID customer(Tenant t){return customers.create(t.hotel(),new CustomerRequest(UUID.randomUUID().toString(),CustomerKind.COMPANY,"Customer",null,null,null,null,true)).id();}
 UUID debt(Tenant t,UUID c,String amount){var f=folios.create(t.hotel(),t.branch(),c);folios.charge(t.hotel(),t.branch(),f.id(),new BigDecimal(amount),"Service",UUID.randomUUID());credit.approveFolio(t.hotel(),t.branch(),f.id());return f.id();}
 CreditRequest settlement(UUID c,String amount,UUID key){return new CreditRequest(c,new BigDecimal(amount),"Settlement",key);}
 List<Map<String,Object>> approvals(Tenant t){return db.queryForList("select id,folio_id,request_id,payload,status from payment_approvals where hotel_id=? order by id",t.hotel());}
 void approve(Tenant t){shifts.open(t.hotel(),t.branch(),new CashierShiftRequest(BigDecimal.ZERO,"Test shift",null));for(var row:approvals(t))payments.approve(t.hotel(),t.branch(),(UUID)row.get("id"),true,"Owner verified receipt");}
 void concurrently(Tenant t,Runnable action) throws Exception {
  var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
  try {
   var tasks=new ArrayList<Future<?>>();
   for(int i=0;i<2;i++)tasks.add(pool.submit(()->{as(t);ready.countDown();try{if(!start.await(30,TimeUnit.SECONDS))throw new AssertionError("Race did not start");action.run();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}finally{SecurityContextHolder.clearContext();}}));
   assertTrue(ready.await(30,TimeUnit.SECONDS));start.countDown();for(var task:tasks)task.get(60,TimeUnit.SECONDS);
  }finally{start.countDown();pool.shutdownNow();}
 }
 @Test void transferExactReplayMovesOnlyOnce(){var t=tenant();var b=branch(t);var p=product(t);opening(t,p);var key=UUID.randomUUID();transfer(t,b,p,"3",key);transfer(t,b,p,"3.0000",key);stock(t,t.branch(),p,"7");stock(t,b,p,"3");assertEquals(2,movements(t,key));}
 @Test void transferRejectsChangedDestination(){var t=tenant();var b=branch(t);var c=branch(t);var p=product(t);opening(t,p);var key=UUID.randomUUID();transfer(t,b,p,"3",key);assertThrows(BusinessRuleException.class,()->transfer(t,c,p,"3",key));stock(t,c,p,"0");assertEquals(2,movements(t,key));}
 @Test void transferRejectsChangedProduct(){var t=tenant();var b=branch(t);var p=product(t);var other=product(t);opening(t,p);opening(t,other);var key=UUID.randomUUID();transfer(t,b,p,"3",key);assertThrows(BusinessRuleException.class,()->transfer(t,b,other,"3",key));stock(t,t.branch(),other,"10");}
 @Test void transferRejectsChangedQuantityAndReason(){var t=tenant();var b=branch(t);var p=product(t);opening(t,p);var key=UUID.randomUUID();transfer(t,b,p,"3",key);assertThrows(BusinessRuleException.class,()->transfer(t,b,p,"4",key));assertThrows(BusinessRuleException.class,()->inventory.transfer(t.hotel(),t.branch(),b,p,new BigDecimal("3"),key,"Changed"));assertEquals(2,movements(t,key));}
 @Test void insufficientTransferLeavesNoMovementsAndCanRetry(){var t=tenant();var b=branch(t);var p=product(t);var key=UUID.randomUUID();assertEquals("INSUFFICIENT_STOCK",assertThrows(ApiException.class,()->transfer(t,b,p,"3",key)).code());assertEquals(0,movements(t,key));opening(t,p);transfer(t,b,p,"3",key);assertEquals(2,movements(t,key));}
 @Test void concurrentTransferReplayMovesOnlyOnce() throws Exception {var t=tenant();var b=branch(t);var p=product(t);opening(t,p);var key=UUID.randomUUID();concurrently(t,()->transfer(t,b,p,"3",key));assertEquals(2,movements(t,key));stock(t,t.branch(),p,"7");stock(t,b,p,"3");}
 @Test void rollbackRemovesBothTransferLegs(){var t=tenant();var b=branch(t);var p=product(t);opening(t,p);var key=UUID.randomUUID();assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).executeWithoutResult(s->{transfer(t,b,p,"3",key);throw new IllegalStateException("Simulated transaction failure");}));assertEquals(0,movements(t,key));stock(t,t.branch(),p,"10");stock(t,b,p,"0");transfer(t,b,p,"3",key);assertEquals(2,movements(t,key));}
 @Test void transferRejectsForeignTenantAndBranch(){var a=tenant();var b=branch(a);var p=product(a);opening(a,p);var other=tenant();assertThrows(AccessDeniedException.class,()->transfer(a,b,p,"3",UUID.randomUUID()));as(a);assertThrows(RuntimeException.class,()->transfer(a,other.branch(),p,"3",UUID.randomUUID()));stock(a,a.branch(),p,"10");}
 @Test void partialSettlementKeepsApprovalPendingAndReplays(){var t=tenant();var c=customer(t);debt(t,c,"100");var q=settlement(c,"30",UUID.randomUUID());var response=credit.settle(t.hotel(),t.branch(),q);var plan=approvals(t);assertEquals(1,plan.size());assertEquals("PENDING",plan.getFirst().get("status"));assertEquals(response,credit.settle(t.hotel(),t.branch(),q));assertEquals(plan,approvals(t));assertEquals(0,db.queryForObject("select count(*) from payments where hotel_id=?",Integer.class,t.hotel()));}
 @Test void processedSettlementReplayNeverReallocatesToAnotherFolio(){var t=tenant();var c=customer(t);debt(t,c,"100");debt(t,c,"100");var q=settlement(c,"100",UUID.randomUUID());var response=credit.settle(t.hotel(),t.branch(),q);approve(t);var plan=approvals(t);assertEquals(response,credit.settle(t.hotel(),t.branch(),q));assertEquals(plan,approvals(t));assertEquals(1,db.queryForObject("select count(*) from payments where hotel_id=?",Integer.class,t.hotel()));}
 @Test void settlementAcrossFoliosFreezesPlanAfterBalanceChanges(){var t=tenant();var c=customer(t);debt(t,c,"100");debt(t,c,"100");var q=settlement(c,"150",UUID.randomUUID());var response=credit.settle(t.hotel(),t.branch(),q);assertEquals(2,approvals(t).size());approve(t);var plan=approvals(t);assertEquals(response,credit.settle(t.hotel(),t.branch(),q));assertEquals(plan,approvals(t));}
 @Test void settlementRejectsChangedDescriptionAndCustomer(){var t=tenant();var c=customer(t);debt(t,c,"100");var q=settlement(c,"30",UUID.randomUUID());credit.settle(t.hotel(),t.branch(),q);assertThrows(BusinessRuleException.class,()->credit.settle(t.hotel(),t.branch(),new CreditRequest(c,q.amount(),"Changed",q.sourceId())));var other=customer(t);debt(t,other,"100");assertThrows(BusinessRuleException.class,()->credit.settle(t.hotel(),t.branch(),settlement(other,"30",q.sourceId())));assertEquals(1,approvals(t).size());}
 @Test void settlementRejectsChangedAmount(){var t=tenant();var c=customer(t);debt(t,c,"100");var key=UUID.randomUUID();credit.settle(t.hotel(),t.branch(),settlement(c,"30",key));assertThrows(IllegalStateException.class,()->credit.settle(t.hotel(),t.branch(),settlement(c,"40",key)));assertEquals(1,approvals(t).size());}
 @Test void concurrentSettlementReplayCreatesOneApproval() throws Exception {var t=tenant();var c=customer(t);debt(t,c,"100");var q=settlement(c,"30",UUID.randomUUID());concurrently(t,()->credit.settle(t.hotel(),t.branch(),q));assertEquals(1,approvals(t).size());}
 @Test void settlementRejectsForeignTenant(){var a=tenant();var c=customer(a);debt(a,c,"100");var other=tenant();assertThrows(AccessDeniedException.class,()->credit.settle(a.hotel(),a.branch(),settlement(c,"30",UUID.randomUUID())));assertThrows(ApiException.class,()->credit.settle(other.hotel(),other.branch(),settlement(c,"30",UUID.randomUUID())));assertTrue(approvals(a).isEmpty());}
 @Test void restrictedBranchAccessIsCheckedOnReplay(){
  var t=tenant();var b=branch(t);var p=product(t);opening(t,p);var key=UUID.randomUUID();transfer(t,b,p,"3",key);
  var c=customer(t);debt(t,c,"100");var q=settlement(c,"30",UUID.randomUUID());credit.settle(t.hotel(),t.branch(),q);
  db.update("update hotel_memberships set all_branches=false where hotel_id=? and user_id=?",t.hotel(),t.user());
  assertThrows(AccessDeniedException.class,()->transfer(t,b,p,"3",key));
  assertThrows(AccessDeniedException.class,()->credit.settle(t.hotel(),t.branch(),q));
  db.update("insert into membership_branch_access(id,membership_id,hotel_id,branch_id) select ?,id,hotel_id,? from hotel_memberships where hotel_id=? and user_id=?",UUID.randomUUID(),t.branch(),t.hotel(),t.user());
  assertThrows(AccessDeniedException.class,()->transfer(t,b,p,"3",key));
  assertNotNull(credit.settle(t.hotel(),t.branch(),q));
 }
 @Test void legacyTransferPairIsAdoptedWithoutNewMovements(){
  var t=tenant();var b=branch(t);var p=product(t);opening(t,p);var key=UUID.randomUUID();
  new TransactionTemplate(transactions).executeWithoutResult(s->{
   inventory.move(t.hotel(),t.branch(),p,new BigDecimal("-3"),MovementKind.TRANSFER_OUT,key,t.user(),"Transfer");
   inventory.move(t.hotel(),b,p,new BigDecimal("3"),MovementKind.TRANSFER_IN,key,t.user(),"Transfer");
  });
  transfer(t,b,p,"3",key);assertEquals(2,movements(t,key));
  var other=branch(t);assertThrows(BusinessRuleException.class,()->transfer(t,other,p,"3",key));
 }
 @Test void ambiguousLegacyTransferIsNotRepairedByRetry(){
  var t=tenant();var b=branch(t);var p=product(t);opening(t,p);var key=UUID.randomUUID();
  new TransactionTemplate(transactions).executeWithoutResult(s->inventory.move(t.hotel(),t.branch(),p,new BigDecimal("-3"),MovementKind.TRANSFER_OUT,key,t.user(),"Transfer"));
  assertEquals("LEGACY_TRANSFER_CONFLICT",assertThrows(BusinessRuleException.class,()->transfer(t,b,p,"3",key)).code());
  assertEquals(1,movements(t,key));stock(t,b,p,"0");
  assertEquals(0,db.queryForObject("select count(*) from inventory_transfer_commands where hotel_id=? and request_id=?",Integer.class,t.hotel(),key));
 }
 @Test void historicalSettlementCannotBeReallocatedOrReassigned(){
  var t=tenant();var c=customer(t);var f=debt(t,c,"100");var key=UUID.randomUUID();
  var child=UUID.nameUUIDFromBytes((key+":"+f).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  payments.request(t.hotel(),t.branch(),new PaymentWorkflow.Request(f,child,List.of(new PaymentWorkflow.Part(com.hotelmanagement.hms.payment.model.PaymentMethod.CASH,new BigDecimal("30")))));
  var before=approvals(t);var other=customer(t);debt(t,other,"100");
  for(var customer:List.of(c,other))assertEquals("LEGACY_SETTLEMENT_REVIEW_REQUIRED",assertThrows(BusinessRuleException.class,()->credit.settle(t.hotel(),t.branch(),settlement(customer,"30",key))).code());
  assertEquals(before,approvals(t));
  assertEquals(0,db.queryForObject("select count(*) from credit_settlement_commands where hotel_id=?",Integer.class,t.hotel()));
 }
 @Test void settlementRollbackRemovesParentAndEveryApproval(){
  var t=tenant();var c=customer(t);debt(t,c,"100");debt(t,c,"100");var q=settlement(c,"150",UUID.randomUUID());
  assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).executeWithoutResult(s->{credit.settle(t.hotel(),t.branch(),q);throw new IllegalStateException("Simulated transaction failure");}));
  assertTrue(approvals(t).isEmpty());
  assertEquals(0,db.queryForObject("select count(*) from credit_settlement_commands where hotel_id=?",Integer.class,t.hotel()));
  credit.settle(t.hotel(),t.branch(),q);assertEquals(2,approvals(t).size());
 }
 @Test void concurrentConflictingTransferCommandsHaveOnlyOneWinner() throws Exception {
  var t=tenant();var b=branch(t);var p=product(t);var other=product(t);opening(t,p);opening(t,other);var key=UUID.randomUUID();
  var sequence=new java.util.concurrent.atomic.AtomicInteger();var rejected=new java.util.concurrent.atomic.AtomicInteger();
  concurrently(t,()->{var selected=sequence.getAndIncrement()==0?p:other;try{transfer(t,b,selected,"3",key);}catch(BusinessRuleException expected){rejected.incrementAndGet();}});
  assertEquals(1,rejected.get());assertEquals(2,movements(t,key));
 }
 @Test void concurrentDistinctTransfersCannotOverdrawStock() throws Exception {
  var t=tenant();var b=branch(t);var p=product(t);opening(t,p);var rejected=new java.util.concurrent.atomic.AtomicInteger();
  concurrently(t,()->{try{transfer(t,b,p,"7",UUID.randomUUID());}catch(ApiException e){assertEquals("INSUFFICIENT_STOCK",e.code());rejected.incrementAndGet();}});
  assertEquals(1,rejected.get());stock(t,t.branch(),p,"3");stock(t,b,p,"7");
 }
 @Test void parentCommandsAndAllocationsAreImmutable(){
  var t=tenant();var b=branch(t);var p=product(t);opening(t,p);var key=UUID.randomUUID();transfer(t,b,p,"3",key);
  var c=customer(t);debt(t,c,"100");credit.settle(t.hotel(),t.branch(),settlement(c,"30",UUID.randomUUID()));
  for(var table:List.of("inventory_transfer_commands","credit_settlement_commands","credit_settlement_allocations")){
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("delete from "+table+" where hotel_id=?",t.hotel()));
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("update "+table+" set request_id=? where hotel_id=?",UUID.randomUUID(),t.hotel()));
  }
 }
}
