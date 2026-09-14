package com.hotelmanagement.hms;
import com.hotelmanagement.hms.platform.onboarding.service.OwnerOnboardingService;
import com.hotelmanagement.hms.platform.onboarding.dto.*;
import com.hotelmanagement.hms.platform.dto.*;
import com.hotelmanagement.hms.platform.service.HotelAdministrationService;
import com.hotelmanagement.hms.customer.dto.*;
import com.hotelmanagement.hms.customer.model.*;
import com.hotelmanagement.hms.customer.service.CustomerService;
import com.hotelmanagement.hms.room.dto.*;
import com.hotelmanagement.hms.room.service.RoomService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"hms.security.jwt.secret=operational-integration-test-secret-at-least-32-bytes","logging.level.root=WARN","logging.level.com.hotelmanagement.hms=WARN","management.otlp.metrics.export.enabled=false"})

class OperationsTest {

 com.hotelmanagement.hms.reservation.dto.ReservationRequest booking(UUID customer,UUID room,java.time.LocalDate start,java.time.LocalDate end) {
  return new com.hotelmanagement.hms.reservation.dto.ReservationRequest(customer,start,end,
    List.of(new com.hotelmanagement.hms.reservation.dto.ReservationRequest.RoomBooking(room,1,0,null,List.of())),null);
 }
 @Test void reservationIntervalsCancellationAndTenantIsolation() {
  var a=tenant();var c=customer(a);var r=room(a,"101");var date=java.time.LocalDate.now().plusDays(3);
  var first=reservations.create(a.hotel(),a.branch(),booking(c.id(),r.id(),date,date.plusDays(2)));
  assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->reservations.create(a.hotel(),a.branch(),booking(c.id(),r.id(),date.plusDays(1),date.plusDays(3))));
  assertEquals(0,reservations.available(a.hotel(),a.branch(),date,date.plusDays(2),1,0,0,50).totalElements());
  assertNotNull(reservations.create(a.hotel(),a.branch(),booking(c.id(),r.id(),date.plusDays(2),date.plusDays(3))));
  reservations.cancel(a.hotel(),a.branch(),first.id());
  assertEquals(1,reservations.available(a.hotel(),a.branch(),date,date.plusDays(2),1,0,0,50).totalElements());
  var b=tenant();assertThrows(org.springframework.security.access.AccessDeniedException.class,()->reservations.get(a.hotel(),a.branch(),first.id()));
  assertThrows(com.hotelmanagement.hms.shared.web.ApiException.class,()->reservations.get(b.hotel(),b.branch(),first.id()));
 }
 @Test void concurrentBookingsCannotOverlap() throws Exception {
  var a=tenant();var c=customer(a);var r=room(a,"201");var date=java.time.LocalDate.now().plusDays(10);
  var request=booking(c.id(),r.id(),date,date.plusDays(2));
  var start=new java.util.concurrent.CountDownLatch(1);
  try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){
   java.util.concurrent.Callable<Boolean> call=()->{as(a);start.await();try{reservations.create(a.hotel(),a.branch(),request);return true;}
    catch(org.springframework.dao.DataIntegrityViolationException e){return false;}finally{clear();}};
   var first=executor.submit(call);var second=executor.submit(call);start.countDown();
   assertNotEquals(first.get(40,java.util.concurrent.TimeUnit.SECONDS),second.get(40,java.util.concurrent.TimeUnit.SECONDS));
  }
 }
 @Test void folioLedgerReversalsTransfersAndTenantScope() {
  var a=tenant();var c=customer(a);var first=folios.create(a.hotel(),a.branch(),c.id());var second=folios.create(a.hotel(),a.branch(),c.id());
  var entry=folios.charge(a.hotel(),a.branch(),first.id(),new BigDecimal("100000"),"Charge",UUID.randomUUID());
  folios.transfer(a.hotel(),a.branch(),first.id(),second.id(),new BigDecimal("25000"),"Split bill",UUID.randomUUID());
  assertEquals(0,new BigDecimal("75000").compareTo(folios.get(a.hotel(),a.branch(),first.id()).balance()));
  assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("delete from folio_entries where id=?",entry.id()));
  var b=tenant();assertThrows(org.springframework.security.access.AccessDeniedException.class,()->folios.get(a.hotel(),a.branch(),first.id()));
  as(a);var adjustment=folios.charge(a.hotel(),a.branch(),first.id(),new BigDecimal("50"),"Correction",UUID.randomUUID());
  folios.reverse(a.hotel(),a.branch(),first.id(),adjustment.id(),"Mistake");
  assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->folios.reverse(a.hotel(),a.branch(),first.id(),adjustment.id(),"Again"));
 }
 @Test void checkInSnapshotsAccommodationAndUnpaidCheckoutRollsBack() {
  var a=tenant();var c=customer(a);var room=room(a,"301");var date=java.time.LocalDate.now();
  var reservation=reservations.create(a.hotel(),a.branch(),booking(c.id(),room.id(),date,date.plusDays(2)));
  rooms.rate(a.hotel(),a.branch(),room.roomTypeId(),new BigDecimal("12000"));
  assertEquals(1,stays.checkIn(a.hotel(),a.branch(),reservation.id()).size());
  assertEquals(0,new BigDecimal("20000").compareTo(folios.get(a.hotel(),a.branch(),reservation.folioId()).balance()));
  assertThrows(com.hotelmanagement.hms.shared.web.ApiException.class,()->stays.checkOut(a.hotel(),a.branch(),reservation.id()));
  assertEquals(com.hotelmanagement.hms.reservation.model.ReservationStatus.CHECKED_IN,reservations.get(a.hotel(),a.branch(),reservation.id()).status());
  assertEquals(com.hotelmanagement.hms.room.model.HousekeepingState.CLEAN,rooms.get(a.hotel(),a.branch(),room.id()).housekeeping());
 }
 @Autowired com.hotelmanagement.hms.payment.service.PaymentWorkflow paymentWorkflow;
 @Autowired com.hotelmanagement.hms.payment.service.PaymentService paymentService;
 @Autowired com.hotelmanagement.hms.payment.service.CashierShiftService shifts;
 @Autowired com.hotelmanagement.hms.report.web.AccountingReportController accounting;
 @Autowired com.hotelmanagement.hms.report.web.ExpenseController expenses;
 @Test void ownerOverrideNeedsReasonAndShiftAndReportsUseLedgerMovements(){
  var a=tenant();var customer=customer(a);var folio=folios.create(a.hotel(),a.branch(),customer.id());
  folios.charge(a.hotel(),a.branch(),folio.id(),new BigDecimal("100"),"Service",UUID.randomUUID());
  var pending=paymentWorkflow.request(a.hotel(),a.branch(),new com.hotelmanagement.hms.payment.service.PaymentWorkflow.Request(folio.id(),UUID.randomUUID(),List.of(new com.hotelmanagement.hms.payment.service.PaymentWorkflow.Part(com.hotelmanagement.hms.payment.model.PaymentMethod.CASH,new BigDecimal("50"))),"FOOD"));
  UUID approval=(UUID)pending.get("id");
  assertThrows(IllegalStateException.class,()->paymentWorkflow.approve(a.hotel(),a.branch(),approval,true,null));
  assertThrows(com.hotelmanagement.hms.shared.web.ApiException.class,()->paymentWorkflow.approve(a.hotel(),a.branch(),approval,true,"Owner verified receipt"));
  shifts.open(a.hotel(),a.branch(),new com.hotelmanagement.hms.payment.dto.CashierShiftRequest(BigDecimal.ZERO,"Test shift",null));
  paymentWorkflow.approve(a.hotel(),a.branch(),approval,true,"Owner verified receipt");
  assertEquals(0,new BigDecimal("50").compareTo(folios.get(a.hotel(),a.branch(),folio.id()).balance()));
  assertEquals("Owner verified receipt",jdbc.queryForObject("select reason from audit_events where entity_id=? and action='OWNER_PAYMENT_DECISION'",String.class,approval));
  var today=java.time.LocalDate.now(java.time.ZoneId.of("Africa/Kigali"));
  expenses.create(a.hotel(),a.branch(),new com.hotelmanagement.hms.report.web.ExpenseController.Expense(today,"UTILITIES","Service expense",new BigDecimal("20"),com.hotelmanagement.hms.payment.model.PaymentMethod.CASH,UUID.randomUUID()));
  UUID payment=jdbc.queryForObject("select id from payments where folio_id=?",UUID.class,folio.id());
  paymentService.refund(a.hotel(),a.branch(),payment);
  var report=accounting.accounting(a.hotel(),a.branch(),today,today);
  var summary=(java.util.Map<?,?>)report.get("summary");
  assertEquals(0,new BigDecimal("50").compareTo((BigDecimal)summary.get("receipts")));
  assertEquals(0,new BigDecimal("50").compareTo((BigDecimal)summary.get("refunds")));
  assertEquals(0,new BigDecimal("-20").compareTo((BigDecimal)summary.get("netCashMovement")));
  assertEquals(0,BigDecimal.ZERO.compareTo((BigDecimal)summary.get("supplierPayments")));
 }
 @DynamicPropertySource static void database(DynamicPropertyRegistry r) { IntegrationServices.configure(r); }
 @Autowired OwnerOnboardingService onboarding;
 @Autowired HotelAdministrationService hotels;
 @Autowired CustomerService customers;
 @Autowired RoomService rooms;
 @Autowired com.hotelmanagement.hms.reservation.service.ReservationService reservations;
 @Autowired com.hotelmanagement.hms.folio.service.FolioService folios;
 @Autowired com.hotelmanagement.hms.stay.service.StayService stays;
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 record Tenant(UUID actor,UUID hotel,UUID branch) {}
 Tenant tenant() {
  String code=UUID.randomUUID().toString();
  var owner=onboarding.signup(new OwnerSignupRequest(code+"@example.test","long operational passphrase","Owner",null,"en",
    new CreateHotelRequest(code,"Hotel","Hotel",null,null,null,null,"RWF","Africa/Kigali","en")));
  var branch=hotels.createBranch(owner.ownerUserId(),owner.hotel().id(),new CreateBranchRequest("MAIN","Main",null,null,null,null));
  var result=new Tenant(owner.ownerUserId(),owner.hotel().id(),branch.id()); as(result);return result;
 }
 void as(Tenant t) {
  var jwt=Jwt.withTokenValue("test").header("alg","HS256").subject(t.actor().toString()).build();
  SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,List.of()));
 }
 @AfterEach void clear() { SecurityContextHolder.clearContext(); }
 CustomerResponse customer(Tenant t) { as(t);return customers.create(t.hotel(),new CustomerRequest("CUSTOMER",CustomerKind.COMPANY,"Company",null,null,null,null,true)); }
 RoomResponse room(Tenant t,String code) {
  as(t);var type=rooms.type(t.hotel(),t.branch(),new RoomTypeRequest(code,"Double",null,2,2,1,new BigDecimal("10000")));
  return rooms.create(t.hotel(),t.branch(),new RoomRequest(type.id(),code,null,1,"King","180 x 200 cm",2,1));
 }
 @Test void customersAreReusableAndTenantScoped() {
  var a=tenant();var c=customer(a);var b=tenant();
  assertThrows(org.springframework.security.access.AccessDeniedException.class,()->customers.get(a.hotel(),c.id()));
  assertThrows(com.hotelmanagement.hms.shared.web.ApiException.class,()->customers.get(b.hotel(),c.id()));
  as(a);assertEquals(c.id(),customers.get(a.hotel(),c.id()).id());
  assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->customer(a));
 }
 @Test void roomsEnforceTypeScopeAndCapacity() {
  var a=tenant();var r=room(a,"101");var b=tenant();
  assertThrows(org.springframework.security.access.AccessDeniedException.class,()->rooms.get(a.hotel(),a.branch(),r.id()));
  assertThrows(com.hotelmanagement.hms.shared.web.ApiException.class,()->rooms.create(b.hotel(),b.branch(),new RoomRequest(r.roomTypeId(),"X",null,1,"King",null,2,0)));
  as(a);assertThrows(IllegalArgumentException.class,()->rooms.create(a.hotel(),a.branch(),new RoomRequest(r.roomTypeId(),"102",null,1,"King",null,3,0)));
  assertEquals(1,rooms.rooms(a.hotel(),a.branch(),0,50).totalElements());
 }
}


