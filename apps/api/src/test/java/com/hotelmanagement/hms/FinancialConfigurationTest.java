package com.hotelmanagement.hms;

import com.hotelmanagement.hms.platform.onboarding.service.OwnerOnboardingService;
import com.hotelmanagement.hms.platform.onboarding.dto.OwnerSignupRequest;
import com.hotelmanagement.hms.platform.dto.*;
import com.hotelmanagement.hms.platform.service.HotelAdministrationService;
import com.hotelmanagement.hms.platform.currency.service.HotelCurrencyService;
import com.hotelmanagement.hms.platform.currency.dto.ExchangeRateRequest;
import com.hotelmanagement.hms.payment.service.*;
import com.hotelmanagement.hms.payment.service.PaymentAccountService.Account;
import com.hotelmanagement.hms.payment.model.PaymentMethod;
import com.hotelmanagement.hms.product.dto.ProductRequest;
import com.hotelmanagement.hms.product.model.Destination;
import com.hotelmanagement.hms.product.service.ProductService;
import com.hotelmanagement.hms.purchase.service.PurchaseOrderService;
import com.hotelmanagement.hms.purchase.dto.PurchaseOrderRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"hms.security.jwt.secret=financial-integration-only-secret-at-least-32-bytes","logging.level.root=WARN","logging.level.com.hotelmanagement.hms=WARN","management.otlp.metrics.export.enabled=false"})
class FinancialConfigurationTest {
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){IntegrationServices.configure(r);}
 @Autowired tools.jackson.databind.json.JsonMapper json;
 @Autowired PaymentService paymentService;
 @Autowired com.hotelmanagement.hms.payment.web.PaymentController paymentController;
 @Autowired com.hotelmanagement.hms.payment.web.PaymentAccountController accountController;
 @Autowired com.hotelmanagement.hms.report.web.ReportController reports;
 @Autowired OwnerOnboardingService onboarding;@Autowired HotelAdministrationService hotels;
 @Autowired HotelCurrencyService currencies;@Autowired PaymentAccountService accounts;
 @Autowired com.hotelmanagement.hms.nonresident.service.NonResidentBillService bills;
 @Autowired ProductService products;@Autowired PurchaseOrderService purchases;@Autowired JdbcTemplate db;
 @Autowired com.hotelmanagement.hms.vendor.service.VendorService vendors;
 @Autowired com.hotelmanagement.hms.customer.service.CustomerService customers;
 @Autowired com.hotelmanagement.hms.folio.service.FolioService folios;
 @Autowired com.hotelmanagement.hms.order.service.OrderService orders;
 @Autowired PaymentWorkflow workflow;@Autowired CashierShiftService shifts;
 @Autowired com.hotelmanagement.hms.report.web.ExpenseController expenses;
 void safe(Object response){assertFalse(json.writeValueAsString(response).contains("00001234"));}
 record Tenant(UUID user,UUID hotel,UUID branch){}
 Tenant tenant(){String code=UUID.randomUUID().toString();var owner=onboarding.signup(new OwnerSignupRequest(code+"@example.test","a secure hotel passphrase","Owner",null,"en",new CreateHotelRequest(code,"Hotel","Hotel",null,null,null,null,"RWF","Africa/Kigali","en")));var branch=hotels.createBranch(owner.ownerUserId(),owner.hotel().id(),new CreateBranchRequest("MAIN","Main",null,null,null,null));var t=new Tenant(owner.ownerUserId(),owner.hotel().id(),branch.id());as(t);return t;}
 void as(Tenant t){SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg","HS256").subject(t.user().toString()).build(),List.of()));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 void rate(Tenant t,String value){hotels.createRate(t.user(),t.hotel(),new ExchangeRateRequest("USD",new BigDecimal(value),OffsetDateTime.now().minusSeconds(1)));}
 Account account(Tenant t){return accounts.save(t.hotel(),null,new Account(null,"BK USD",PaymentMethod.BANK_TRANSFER,"USD","ACCOUNT_NUMBER","00001234",true));}
 void money(String expected,Object actual){assertEquals(0,new BigDecimal(expected).compareTo((BigDecimal)actual));}
 UUID customer(Tenant t){return customers.create(t.hotel(),new com.hotelmanagement.hms.customer.dto.CustomerRequest("C",com.hotelmanagement.hms.customer.model.CustomerKind.COMPANY,"Customer",null,null,null,null,true)).id();}
 @Test void accountsEnforceCurrencyTenantTypeAndStatus(){
  var a=tenant();rate(a,"1500");var account=account(a);
  safe(accounts.list(a.hotel()));safe(accountController.accounts(a.hotel()));
  assertEquals("****1234",accounts.list(a.hotel()).getFirst().maskedIdentifier());
  assertEquals("00001234",accounts.resolve(a.hotel(),account.id(),PaymentMethod.BANK_TRANSFER,"USD").identifier());
  assertThrows(IllegalArgumentException.class,()->accounts.resolve(a.hotel(),account.id(),PaymentMethod.BANK_TRANSFER,"RWF"));
  assertThrows(IllegalArgumentException.class,()->accounts.resolve(a.hotel(),account.id(),PaymentMethod.CASH,"USD"));
  var b=tenant();assertThrows(com.hotelmanagement.hms.shared.web.ApiException.class,()->accounts.resolve(b.hotel(),account.id(),PaymentMethod.BANK_TRANSFER,"USD"));
  assertThrows(org.springframework.security.access.AccessDeniedException.class,()->accounts.save(a.hotel(),account.id(),account));
  as(a);accounts.save(a.hotel(),account.id(),new Account(account.id(),account.name(),account.method(),account.currency(),account.identifierType(),account.identifier(),false));
  assertThrows(IllegalArgumentException.class,()->accounts.resolve(a.hotel(),account.id(),account.method(),account.currency()));
  assertThrows(IllegalArgumentException.class,()->accounts.save(a.hotel(),null,new Account(null,"MTN",PaymentMethod.MOBILE_MONEY,"RWF","ACCOUNT_NUMBER","123",true)));
  assertNotNull(accounts.save(a.hotel(),null,new Account(null,"MTN",PaymentMethod.MOBILE_MONEY,"RWF","MERCHANT_CODE","00123",true)));
  assertThrows(IllegalArgumentException.class,()->currencies.save(a.hotel(),"RWF","Rwandan franc",false));
 }
 @Test void paymentFreezesAccountAndRateAcrossRenameAndRetry(){
  var t=tenant();rate(t,"1500");var account=account(t);var folio=folios.create(t.hotel(),t.branch(),customer(t));
  folios.charge(t.hotel(),t.branch(),folio.id(),new BigDecimal("5000"),"Service",UUID.randomUUID());
  var request=new PaymentWorkflow.Request(folio.id(),UUID.randomUUID(),List.of(new PaymentWorkflow.Part(PaymentMethod.BANK_TRANSFER,"USD",BigDecimal.ONE,account.id())),"FOOD");
  var approval=workflow.request(t.hotel(),t.branch(),request);
  assertTrue(db.queryForObject("select payload from payment_approvals where id=?",String.class,approval.get("id")).contains("00001234"));
  safe(workflow.list(t.hotel(),t.branch()));
  assertTrue(json.writeValueAsString(workflow.list(t.hotel(),t.branch())).contains("****1234"));
  rate(t,"1600");accounts.save(t.hotel(),account.id(),new Account(account.id(),"Renamed BK",account.method(),account.currency(),account.identifierType(),account.identifier(),false));
  assertEquals(approval.get("id"),workflow.request(t.hotel(),t.branch(),request).get("id"));
  safe(workflow.request(t.hotel(),t.branch(),request));
  var different=new PaymentWorkflow.Request(folio.id(),request.requestId(),List.of(new PaymentWorkflow.Part(PaymentMethod.BANK_TRANSFER,"USD",BigDecimal.ONE,null)),"FOOD");
  assertThrows(IllegalStateException.class,()->workflow.request(t.hotel(),t.branch(),different));
  shifts.open(t.hotel(),t.branch(),new com.hotelmanagement.hms.payment.dto.CashierShiftRequest(BigDecimal.ZERO,"Test",null));
  workflow.approve(t.hotel(),t.branch(),(UUID)approval.get("id"),true,"Owner verified bank receipt");
  var payment=db.queryForMap("select * from payments where folio_id=?",folio.id());money("1500",payment.get("base_amount"));assertEquals("BK USD",payment.get("payment_account_name"));assertEquals(account.id(),payment.get("payment_account_id"));
  assertEquals("00001234",payment.get("payment_account_identifier"));
  var response=paymentService.get(t.hotel(),t.branch(),(UUID)payment.get("id"));safe(response);assertEquals("****1234",response.paymentAccountIdentifier());
  safe(reports.transactions(t.hotel(),t.branch(),LocalDate.now().minusDays(1),LocalDate.now().plusDays(1),PaymentMethod.BANK_TRANSFER));
 }
 @Test void productSalesAndPurchasesKeepCurrencySnapshots(){
  var t=tenant();rate(t,"1500");var account=account(t);
  var p=products.create(t.hotel(),new ProductRequest("SKU","Product","Food","UNIT","UNIT","UNIT",BigDecimal.ONE,BigDecimal.ONE,new BigDecimal("2"),new BigDecimal("3"),BigDecimal.ZERO,false,true,true,true,BigDecimal.ZERO,Destination.SERVICE,"USD","USD"));
  money("4500",p.sellingBasePrice());
  var customer=customer(t);var bill=bills.create(t.hotel(),t.branch(),new com.hotelmanagement.hms.nonresident.dto.NonResidentBillRequest(customer,com.hotelmanagement.hms.nonresident.model.NonResidentBillType.RESTAURANT,null,null));
  var sale=orders.create(t.hotel(),t.branch(),new com.hotelmanagement.hms.order.dto.OrderRequest(customer,bill.folioId(),"SERVICE",List.of(new com.hotelmanagement.hms.order.dto.OrderRequest.Item(p.id(),BigDecimal.ONE))));money("4500",sale.total());
  var item=db.queryForMap("select * from order_items where order_id=?",sale.id());assertEquals("USD",item.get("original_currency"));money("3",item.get("original_unit_price"));money("1500",item.get("fx_rate"));
  var vendor=vendors.create(t.hotel(),new com.hotelmanagement.hms.vendor.dto.VendorRequest("V","Vendor",null,null,null,30,true));
  var purchase=purchases.create(t.hotel(),t.branch(),new PurchaseOrderRequest(vendor.id(),"INV-1",List.of(new PurchaseOrderRequest.Item(p.id(),BigDecimal.ONE,new BigDecimal("2"))),"USD"));
  purchases.approve(t.hotel(),t.branch(),purchase.id());rate(t,"1600");
  var payment=new PurchaseOrderService.VendorPayment(new BigDecimal("2"),PaymentMethod.BANK_TRANSFER,UUID.randomUUID(),"USD",account.id());
  purchases.pay(t.hotel(),t.branch(),purchase.id(),payment);safe(purchases.pay(t.hotel(),t.branch(),purchase.id(),payment));
  var paid=db.queryForMap("select * from vendor_payments where purchase_order_id=?",purchase.id());money("3000",paid.get("amount"));money("3200",paid.get("actual_base_amount"));money("200",paid.get("fx_difference"));
  assertEquals("00001234",paid.get("payment_account_identifier"));safe(purchases.detail(t.hotel(),t.branch(),purchase.id()));
  assertTrue(json.writeValueAsString(purchases.detail(t.hotel(),t.branch(),purchase.id())).contains("****1234"));
  money("4500",db.queryForObject("select unit_price from order_items where order_id=?",BigDecimal.class,sale.id()));
  currencies.save(t.hotel(),"USD","US dollar",false);assertThrows(IllegalArgumentException.class,()->currencies.quote(t.hotel(),"USD",BigDecimal.ONE));
 }
 @Test void expenseStoresOriginalAmountAndAccountAndRetriesWithoutRepricing(){
  var t=tenant();rate(t,"1500");var account=account(t);UUID category=UUID.randomUUID();
  db.update("insert into expense_categories(id,hotel_id,name,active) values(?,?,?,true)",category,t.hotel(),"Supplies");
  var req=new com.hotelmanagement.hms.report.web.ExpenseController.ExpenseRequest(LocalDate.now(),category,"Supplies",new BigDecimal("2"),"BANK_TRANSFER",UUID.randomUUID(),"USD",account.id());
  expenses.create(t.hotel(),t.branch(),req);rate(t,"1600");safe(expenses.create(t.hotel(),t.branch(),req));
  var expense=db.queryForMap("select * from expenses where request_id=?",req.requestId());money("3000",expense.get("amount"));money("2",expense.get("original_amount"));assertEquals(account.id(),expense.get("payment_account_id"));
  assertEquals("00001234",expense.get("payment_account_identifier"));safe(expenses.list(t.hotel(),t.branch()));
  assertTrue(json.writeValueAsString(expenses.list(t.hotel(),t.branch())).contains("****1234"));
 }
 @Test void accountUpdatesPreserveIdentityWithoutResendingIt(){
  var t=tenant();rate(t,"1500");var a=account(t);
  safe(accountController.update(t.hotel(),a.id(),new Account(null,"New name",null,null,null,null,false)));
  var row=db.queryForMap("select * from payment_accounts where id=?",a.id());
  assertEquals("New name",row.get("name"));assertEquals(false,row.get("active"));
  assertEquals("00001234",row.get("identifier"));assertEquals("USD",row.get("currency"));
  assertEquals("BANK_TRANSFER",row.get("method"));assertEquals("ACCOUNT_NUMBER",row.get("identifier_type"));
  for(var change:List.of(new Account(null,"New name",PaymentMethod.CARD,null,null,null,true),
      new Account(null,"New name",null,"RWF",null,null,true),
      new Account(null,"New name",null,null,"PHONE_NUMBER",null,true),
      new Account(null,"New name",null,null,null,"99999999",true)))
   assertThrows(IllegalArgumentException.class,()->accounts.save(t.hotel(),a.id(),change));
  currencies.save(t.hotel(),"USD","Dollar",false);
  safe(accountController.update(t.hotel(),a.id(),new Account(null,"New name",null,null,null,null,true)));
  assertTrue(db.queryForObject("select active from payment_accounts where id=?",Boolean.class,a.id()));
  safe(accountController.create(t.hotel(),new Account(null,"Cash",PaymentMethod.CASH,"RWF","NONE",null,true)));
 }
 @Test void singlePaymentEndpointValidatesAccountAndAcceptsLegacyNull(){
  var t=tenant();rate(t,"1500");var a=account(t);var folio=folios.create(t.hotel(),t.branch(),customer(t));
  folios.charge(t.hotel(),t.branch(),folio.id(),new BigDecimal("5000"),"Service",UUID.randomUUID());
  assertThrows(IllegalArgumentException.class,()->paymentController.create(t.hotel(),t.branch(),
      new com.hotelmanagement.hms.payment.dto.PaymentRequest(folio.id(),PaymentMethod.CASH,"USD",BigDecimal.ONE,BigDecimal.ONE,null,"wrong-method",a.id())));
  var approval=paymentController.create(t.hotel(),t.branch(),new com.hotelmanagement.hms.payment.dto.PaymentRequest(folio.id(),PaymentMethod.CASH,"RWF",BigDecimal.ONE,BigDecimal.ONE,null,"legacy-null"));
  shifts.open(t.hotel(),t.branch(),new com.hotelmanagement.hms.payment.dto.CashierShiftRequest(BigDecimal.ZERO,"Test",null));
  workflow.approve(t.hotel(),t.branch(),(UUID)approval.get("id"),true,"Verify legacy receipt");
  var id=db.queryForObject("select id from payments where folio_id=?",UUID.class,folio.id());
  assertNull(paymentService.get(t.hotel(),t.branch(),id).paymentAccountIdentifier());
 }
 @Test void databaseRejectsInvalidAccountIdentities(){
  var t=tenant();rate(t,"1500");
  for(var fields:List.of(new String[]{"CREDIT","NONE",null},new String[]{"BANK_TRANSFER","PHONE_NUMBER","12345"},
      new String[]{"MOBILE_MONEY","ACCOUNT_NUMBER","12345"},new String[]{"CASH","NONE","12345"},
      new String[]{"BANK_TRANSFER","ACCOUNT_NUMBER",null},new String[]{"BANK_TRANSFER","ACCOUNT_NUMBER","   "})){
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update(
       "insert into payment_accounts(id,hotel_id,name,method,currency,identifier_type,identifier) values(?,?,?,?,'USD',?,?)",
       UUID.randomUUID(),t.hotel(),UUID.randomUUID().toString(),fields[0],fields[1],fields[2]));
  }
 }
}
