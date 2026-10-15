package com.hotelmanagement.hms;

import com.hotelmanagement.hms.commerce.accounting.*;
import com.hotelmanagement.hms.commerce.catalog.*;
import com.hotelmanagement.hms.commerce.foundation.*;
import com.hotelmanagement.hms.commerce.inventory.*;
import com.hotelmanagement.hms.identity.service.UserAccountService;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"hms.commerce.enabled=true","hms.commerce.self-registration-enabled=false","hms.security.jwt.secret=commerce-isolated-test-secret-at-least-32-bytes","logging.level.root=WARN","management.otlp.metrics.export.enabled=false"})
class CommerceTradingTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){IntegrationServices.configure(r);}
    @Autowired JdbcTemplate db;@Autowired UserAccountService users;@Autowired CommerceFoundationService foundation;
    @Autowired CommerceConfigurationService config;@Autowired CommerceCatalogService catalog;@Autowired CommerceOpeningStockService opening;
    @Autowired CommerceLedgerService ledger;@Autowired CommerceTradingService trading;@Autowired CommerceFinanceService finance;
    @Autowired CommerceStockService stock;@Autowired PlatformTransactionManager transactions;
    @Autowired com.hotelmanagement.hms.platform.service.KbpProvisioningService provisioning;
    @Autowired CommerceTradingController controller;
    void as(UUID actor){org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test").header("alg","HS256").subject(actor.toString()).build(),List.of()));}
    @AfterEach void clear(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    record Tenant(UUID actor,UUID business,UUID branch,UUID location,UUID product,UUID customer,UUID supplier,UUID bank) {}
    BigDecimal n(String n){return new BigDecimal(n);} UUID key(){return UUID.randomUUID();}
    void amount(String expected,BigDecimal actual){assertEquals(0,n(expected).compareTo(actual),expected+" != "+actual);}
    UUID user(){return users.createAccount(key()+"@example.test","isolated commerce passphrase","User",null,"en").getId();}
    Tenant tenant(){
        UUID actor=user(),business=key();db.update("INSERT INTO commerce_businesses(id,name,base_currency) VALUES(?,'Trading test','RWF')",business);
        db.update("INSERT INTO commerce_memberships(business_id,user_id,role) VALUES(?,?,'OWNER')",business,actor);
        for(String module:List.of("CORE","INVENTORY","RETAIL","ACCOUNTING"))db.update("INSERT INTO commerce_entitlements(business_id,module,enabled) VALUES(?,?,true)",business,module);
        UUID branch=(UUID)foundation.addBranch(actor,business,"Main").get("id"),location=(UUID)foundation.addLocation(actor,business,branch,"Store","SHOP_FLOOR").get("id");
        UUID product=(UUID)catalog.addProduct(actor,business,null,"SKU",null,"Product","unit",n("10"),n("20"),null).get("id");
        opening.opening(actor,business,location,product,key(),n("10"));ledger.activate(actor,business,key());
        UUID customer=(UUID)trading.party(actor,business,new CommerceTradingService.Party(key(),"CUSTOMER","Customer",null,n("100000"))).get("id");
        UUID supplier=(UUID)trading.party(actor,business,new CommerceTradingService.Party(key(),"SUPPLIER","Supplier",null,n("0"))).get("id");
        UUID bank=(UUID)trading.account(actor,business,new CommerceTradingService.Account(key(),"Bank","BANK","RWF","Account 1")).get("id");
        return new Tenant(actor,business,branch,location,product,customer,supplier,bank);
    }
    CommerceTradingService.Invoice invoice(Tenant t,String kind,String quantity,UUID request){return new CommerceTradingService.Invoice(request,kind,t.location,kind.equals("SALE")?t.customer:t.supplier,"RWF","REF",null,false,List.of(new CommerceTradingService.Line(t.product,null,n(quantity),kind.equals("SALE")?null:n("12"),null,null)),List.of());}
    UUID post(Tenant t,String kind,String qty){return (UUID)trading.invoice(t.actor,t.business,invoice(t,kind,qty,key())).get("id");}
    UUID line(Tenant t,UUID doc){return db.queryForObject("SELECT id FROM commerce_document_lines WHERE business_id=? AND document_id=?",UUID.class,t.business,doc);}
    BigDecimal stock(Tenant t){return db.queryForObject("SELECT quantity FROM commerce_stock_balances WHERE business_id=? AND location_id=? AND product_id=?",BigDecimal.class,t.business,t.location,t.product);}
    BigDecimal balance(Tenant t,String account){return db.queryForObject("SELECT COALESCE(SUM(debit-credit),0) FROM commerce_journal_lines WHERE business_id=? AND account=?",BigDecimal.class,t.business,account);}
    CommerceTradingService.Settlement settlement(Tenant t,UUID doc,String amount,UUID key){return new CommerceTradingService.Settlement(key,doc,new CommerceTradingService.Tender(t.bank,n(amount),null,"bank ref"),false);}
    @Test void existingBusinessOwnerCanEnableEssentialOperationsWithoutActivatingAccounting() {
        UUID actor=user(),business=key();
        db.update("INSERT INTO commerce_businesses(id,name,base_currency) VALUES(?,'Existing store','RWF')",business);
        db.update("INSERT INTO commerce_memberships(business_id,user_id,role) VALUES(?,?,'OWNER')",business,actor);
        for(String module:List.of("CORE","INVENTORY","RETAIL","ACCOUNTING"))
            db.update("INSERT INTO commerce_entitlements(business_id,module,enabled) VALUES(?,?,?)",business,module,module.equals("CORE"));
        assertEquals(false,foundation.operations(actor,business).get("RETAIL"));
        foundation.enableOperations(actor,business);
        assertEquals(true,foundation.operations(actor,business).get("RETAIL"));
        assertEquals(true,foundation.operations(actor,business).get("ACCOUNTING"));
        assertFalse(ledger.active(business),"Financial posting still needs owner-approved opening valuation");
    }
    @Test void cashierAndStorekeeperRolesCanBothSellAndReceiveWithoutChangingCatalog() {
        var t=tenant();
        UUID staff=config.createStaff(t.actor,t.business,key()+"@example.test","Multi role worker",
            "isolated commerce passphrase",List.of("CASHIER","STOREKEEPER"),List.of(t.branch));
        assertEquals(2,db.queryForObject("SELECT count(*) FROM commerce_membership_roles WHERE business_id=? AND user_id=?",Integer.class,t.business,staff));
        UUID purchase=post(t,"PURCHASE","2");
        trading.receive(staff,t.business,new CommerceTradingService.Receive(key(),purchase,line(t,purchase),n("2")));
        amount("12",stock(t));
        var sale=new CommerceTradingService.Invoice(key(),"SALE",t.location,t.customer,"RWF","WORKER",null,false,
            List.of(new CommerceTradingService.Line(t.product,null,n("1"),null,null,null)),List.of());
        trading.invoice(staff,t.business,sale);
        amount("11",stock(t));
        assertThrows(ApiException.class,()->catalog.addCategory(staff,t.business,"Unauthorized edit",null));
    }
    @Test void bottleBaseUnitAndHalfCrateWholesalePricingDeductExactlyTwelveBottles() {
        var t=tenant();
        var product=new CommerceCatalogService.ProductUpdate("Fanta","24-bottle crates",null,null,n("500"),n("1000"),n("800"),n("0"),n("0"),false,false,n("2"),true);
        UUID fanta=(UUID)catalog.createConfigured(t.actor,t.business,new CommerceCatalogController.NewProduct("FANTA", "bottle",product,
            new CommerceCatalogController.PackageSetting("crate",n("24"),n("22000"),n("20000")),null)).get("id");
        stock.post(t.actor,t.business,new CommerceStockService.Command(key(),t.location,null,fanta,n("24"),"Test stock"));
        var order=new CommerceTradingService.Invoice(key(),"SALE",t.location,t.customer,"RWF","HALF-CRATE",null,true,
            List.of(new CommerceTradingService.Line(fanta,"crate",n("0.5"),null,null,null)),List.of());
        UUID sale=(UUID)trading.invoice(t.actor,t.business,order).get("id");
        amount("10000",db.queryForObject("SELECT total FROM commerce_documents WHERE business_id=? AND id=?",BigDecimal.class,t.business,sale));
        amount("12",db.queryForObject("SELECT quantity FROM commerce_stock_balances WHERE business_id=? AND location_id=? AND product_id=?",BigDecimal.class,t.business,t.location,fanta));
        amount("12",db.queryForObject("SELECT base_quantity FROM commerce_document_lines WHERE business_id=? AND document_id=?",BigDecimal.class,t.business,sale));
        assertEquals(true,trading.invoice(t.actor,t.business,order).get("replayed"));
    }
    @Test void invoiceReceiptAndSupplierPaymentAreSeparateAndReplaySafe(){
        var t=tenant();var p=invoice(t,"PURCHASE","4",key());UUID doc=(UUID)trading.invoice(t.actor,t.business,p).get("id");amount("10",stock(t));amount("-48",balance(t,"PAYABLE"));amount("48",balance(t,"GOODS_CLEARING"));
        assertEquals(true,trading.invoice(t.actor,t.business,p).get("replayed"));
        var receive=new CommerceTradingService.Receive(key(),doc,line(t,doc),n("2"));trading.receive(t.actor,t.business,receive);trading.receive(t.actor,t.business,receive);
        amount("12",stock(t));amount("124",balance(t,"INVENTORY"));amount("24",balance(t,"GOODS_CLEARING"));
        var pay=settlement(t,doc,"24",key());trading.settle(t.actor,t.business,pay);trading.settle(t.actor,t.business,pay);
        amount("-24",balance(t,"PAYABLE"));amount("0",balance(t,"EXPENSE"));amount("124",balance(t,"INVENTORY"));
        assertThrows(ApiException.class,()->trading.settle(t.actor,t.business,settlement(t,doc,"25",pay.requestId())));
        trading.receive(t.actor,t.business,new CommerceTradingService.Receive(key(),doc,line(t,doc),n("2")));amount("14",stock(t));amount("0",balance(t,"GOODS_CLEARING"));
    }
    @Test void salesRecognizeRevenueAndCostOnceAndLaterCollectionsNeverRepostRevenue(){
        var t=tenant();var sale=invoice(t,"SALE","3",key());UUID doc=(UUID)trading.invoice(t.actor,t.business,sale).get("id");trading.invoice(t.actor,t.business,sale);
        amount("7",stock(t));amount("-60",balance(t,"SALES"));amount("30",balance(t,"COGS"));amount("60",balance(t,"RECEIVABLE"));
        var pay=settlement(t,doc,"20",key());trading.settle(t.actor,t.business,pay);trading.settle(t.actor,t.business,pay);amount("40",balance(t,"RECEIVABLE"));amount("-60",balance(t,"SALES"));
        assertEquals("PARTIALLY_PAID",trading.detail(t.actor,t.business,doc).get("payment_status"));
        trading.settle(t.actor,t.business,settlement(t,doc,"40",key()));amount("0",balance(t,"RECEIVABLE"));amount("60",balance(t,"CASH_BANK"));
        assertEquals("SETTLED",trading.detail(t.actor,t.business,doc).get("payment_status"));
    }
    @Test void changedCommandsAndOverpaymentsRollBack(){
        var t=tenant();var sale=invoice(t,"SALE","2",key());trading.invoice(t.actor,t.business,sale);
        assertThrows(ApiException.class,()->trading.invoice(t.actor,t.business,invoice(t,"SALE","3",sale.requestId())));amount("8",stock(t));
        long before=db.queryForObject("SELECT COUNT(*) FROM commerce_financial_commands WHERE business_id=?",Long.class,t.business);
        var failed=new CommerceTradingService.Invoice(key(),"SALE",t.location,null,"RWF",null,null,false,invoice(t,"SALE","1",key()).lines(),List.of(new CommerceTradingService.Tender(t.bank,n("21"),null,null)));
        assertThrows(ApiException.class,()->trading.invoice(t.actor,t.business,failed));amount("8",stock(t));assertEquals(before,db.queryForObject("SELECT COUNT(*) FROM commerce_financial_commands WHERE business_id=?",Long.class,t.business));
        assertThrows(ApiException.class,()->post(t,"SALE","100"));amount("8",stock(t));
    }
    @Test void foreignCurrencySnapshotSurvivesRateAndCatalogChangesAndRecordsSettlementGain(){
        var t=tenant();config.configureCurrency(t.actor,t.business,"USD",true);config.rate(t.actor,t.business,"USD",n("1000"),OffsetDateTime.now().minusDays(2),"Opening rate");
        db.update("UPDATE commerce_products SET retail_price=1000 WHERE id=?",t.product);
        var sale=new CommerceTradingService.Invoice(key(),"SALE",t.location,t.customer,"USD",null,null,false,invoice(t,"SALE","2",key()).lines(),List.of());UUID doc=(UUID)trading.invoice(t.actor,t.business,sale).get("id");
        UUID account=(UUID)trading.account(t.actor,t.business,new CommerceTradingService.Account(key(),"USD bank","BANK","USD","Dollar account")).get("id");
        config.rate(t.actor,t.business,"USD",n("1100"),OffsetDateTime.now().minusDays(1),"Settlement rate");db.update("UPDATE commerce_products SET name='Changed',retail_price=9999 WHERE id=?",t.product);
        trading.settle(t.actor,t.business,new CommerceTradingService.Settlement(key(),doc,new CommerceTradingService.Tender(account,n("2.00"),null,null),false));
        amount("-2000",balance(t,"SALES"));amount("2200",balance(t,"CASH_BANK"));amount("-200",balance(t,"FX_GAIN"));amount("0",balance(t,"RECEIVABLE"));
        assertEquals("Product",db.queryForObject("SELECT description FROM commerce_document_lines WHERE document_id=?",String.class,doc));amount("1000",db.queryForObject("SELECT exchange_rate FROM commerce_documents WHERE id=?",BigDecimal.class,doc));
    }
    @Test void returnsAndRefundsReverseOnlyOriginalSaleAndReplayOnce(){
        var t=tenant();UUID doc=post(t,"SALE","2");trading.settle(t.actor,t.business,settlement(t,doc,"40",key()));
        var ret=new CommerceTradingService.Return(key(),doc,line(t,doc),n("1"),true,"Returned sealed");trading.returnGoods(t.actor,t.business,ret);trading.returnGoods(t.actor,t.business,ret);
        amount("9",stock(t));amount("-20",balance(t,"SALES"));amount("10",balance(t,"COGS"));amount("-20",balance(t,"RECEIVABLE"));
        assertEquals("REFUND_DUE",trading.detail(t.actor,t.business,doc).get("payment_status"));assertEquals("PARTIALLY_RETURNED",trading.detail(t.actor,t.business,doc).get("document_status"));
        var refund=new CommerceTradingService.Settlement(key(),doc,new CommerceTradingService.Tender(t.bank,n("20"),null,"Refund"),true);trading.settle(t.actor,t.business,refund);trading.settle(t.actor,t.business,refund);amount("0",balance(t,"RECEIVABLE"));amount("20",balance(t,"CASH_BANK"));
        assertThrows(ApiException.class,()->trading.returnGoods(t.actor,t.business,new CommerceTradingService.Return(key(),doc,line(t,doc),n("2"),true,"Excess")));
    }
    @Test void supplierReturnRequiresReceivedGoodsAndReversesPayable(){
        var t=tenant();UUID doc=post(t,"PURCHASE","2"),line=line(t,doc);
        assertThrows(ApiException.class,()->trading.returnGoods(t.actor,t.business,new CommerceTradingService.Return(key(),doc,line,n("1"),true,"Return")));
        trading.receive(t.actor,t.business,new CommerceTradingService.Receive(key(),doc,line,n("2")));
        trading.returnGoods(t.actor,t.business,new CommerceTradingService.Return(key(),doc,line,n("1"),true,"Damaged shipment"));amount("11",stock(t));amount("-12",balance(t,"PAYABLE"));amount("0",balance(t,"GOODS_CLEARING"));
    }
    @Test void cashShiftReconcilesAndCannotBeSilentlyChanged(){
        var t=tenant();UUID account=(UUID)trading.account(t.actor,t.business,new CommerceTradingService.Account(key(),"Till","CASH","RWF",null)).get("id");
        var opening=new CommerceFinanceService.Shift(key(),t.branch,account,n("100"));UUID shift=(UUID)finance.open(t.actor,t.business,opening).get("id");finance.open(t.actor,t.business,opening);
        UUID doc=post(t,"SALE","1");trading.settle(t.actor,t.business,new CommerceTradingService.Settlement(key(),doc,new CommerceTradingService.Tender(account,n("20"),shift,null),false));
        finance.cashMove(t.actor,t.business,shift,new CommerceFinanceService.CashMove(key(),n("-10"),"Transfer to safe"));var close=new CommerceFinanceService.Close(key(),n("109"));finance.close(t.actor,t.business,shift,close);finance.close(t.actor,t.business,shift,close);
        amount("110",db.queryForObject("SELECT expected_cash FROM commerce_shifts WHERE id=?",BigDecimal.class,shift));amount("-1",db.queryForObject("SELECT variance FROM commerce_shifts WHERE id=?",BigDecimal.class,shift));
        assertThrows(ApiException.class,()->finance.cashMove(t.actor,t.business,shift,new CommerceFinanceService.CashMove(key(),n("1"),"Forbidden")));
        assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE commerce_shifts SET counted_cash=110 WHERE id=?",shift));
    }
    @Test void cashierHasNoPriceOverrideCrossBranchPurchaseOrRefundPermission(){
        var t=tenant();UUID cashier=config.createStaff(t.actor,t.business,key()+"@example.test","Cashier","isolated cashier passphrase",List.of("CASHIER"),List.of(t.branch));
        var input=invoice(t,"SALE","1",key());var override=new CommerceTradingService.Invoice(input.requestId(),"SALE",t.location,t.customer,"RWF",null,null,false,List.of(new CommerceTradingService.Line(t.product,null,n("1"),n("1"),null,"Secret override")),List.of());
        assertThrows(ApiException.class,()->trading.invoice(cashier,t.business,override));assertThrows(ApiException.class,()->trading.invoice(cashier,t.business,invoice(t,"PURCHASE","1",key())));
        UUID doc=(UUID)trading.invoice(cashier,t.business,input).get("id");assertFalse(((Map<?,?>)((List<?>)trading.detail(cashier,t.business,doc).get("lines")).getFirst()).containsKey("inventory_value"));
        assertThrows(ApiException.class,()->trading.returnGoods(cashier,t.business,new CommerceTradingService.Return(key(),doc,line(t,doc),n("1"),true,"Forbidden")));
        UUID branch=(UUID)foundation.addBranch(t.actor,t.business,"Other").get("id");assertThrows(ApiException.class,()->trading.history(cashier,t.business,branch));
        var other=tenant();assertThrows(ApiException.class,()->trading.detail(other.actor,t.business,doc));UUID admin=user();db.update("INSERT INTO platform_administrators(user_id) VALUES(?)",admin);assertThrows(ApiException.class,()->trading.detail(admin,t.business,doc));
    }
    @Test void concurrentCheckoutReplaysOnceAndConcurrentOverdrawHasOneWinner() throws Exception {
        var t=tenant();var input=invoice(t,"SALE","8",key());var pool=Executors.newFixedThreadPool(2);
        try{var gate=new CountDownLatch(1);var a=pool.submit(()->{gate.await();return trading.invoice(t.actor,t.business,input);});var b=pool.submit(()->{gate.await();return trading.invoice(t.actor,t.business,input);});gate.countDown();assertEquals(a.get().get("id"),b.get().get("id"));amount("2",stock(t));
            var gate2=new CountDownLatch(1);Callable<Boolean> attempt=()->{gate2.await();try{post(t,"SALE","2");return true;}catch(ApiException e){return false;}};var c=pool.submit(attempt);var d=pool.submit(attempt);gate2.countDown();assertNotEquals(c.get(),d.get());amount("0",stock(t));
        }finally{pool.shutdownNow();}
    }
    @Test void journalsEnforceBalanceAndImmutabilityAndBaseCurrencyIsLocked(){
        var t=tenant();UUID doc=post(t,"SALE","1");UUID journal=db.queryForObject("SELECT id FROM commerce_journals WHERE source_id=?",UUID.class,doc);
        assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE commerce_documents SET total=1 WHERE id=?",doc));
        assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE commerce_businesses SET base_currency='USD' WHERE id=?",t.business));
        assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("INSERT INTO commerce_journal_lines(id,business_id,journal_id,branch_id,account,debit,credit) VALUES(?,?,?,?,'SALES',1,0)",key(),t.business,journal,t.branch));
        assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(status->{UUID j=key();db.update("INSERT INTO commerce_journals(id,business_id,source_kind,source_id,description,created_by) VALUES(?,?,'TEST',?,'Unbalanced',?)",j,t.business,key(),t.actor);db.update("INSERT INTO commerce_journal_lines(id,business_id,journal_id,branch_id,account,debit,credit) VALUES(?,?,?,?,'SALES',1,0)",key(),t.business,j,t.branch);return null;}));
    }
    @Test void transfersPreserveValuationWithoutExpenseOrRevenue(){
        var t=tenant();UUID branch=(UUID)foundation.addBranch(t.actor,t.business,"Other").get("id"),location=(UUID)foundation.addLocation(t.actor,t.business,branch,"Other store","WAREHOUSE").get("id");
        amount("100",db.queryForObject("SELECT inventory_value FROM commerce_opening_values WHERE business_id=? AND location_id=? AND product_id=?",BigDecimal.class,t.business,t.location,t.product));
        assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE commerce_opening_values SET unit_cost=999 WHERE business_id=?",t.business));
        var command=new CommerceStockService.Command(key(),t.location,location,t.product,n("3"),"Restock branch");stock.post(t.actor,t.business,command);stock.post(t.actor,t.business,command);
        amount("100",balance(t,"INVENTORY"));amount("0",balance(t,"INTERBRANCH"));amount("0",balance(t,"EXPENSE"));amount("0",balance(t,"SALES"));amount("30",db.queryForObject("SELECT inventory_value FROM commerce_stock_balances WHERE business_id=? AND location_id=? AND product_id=?",BigDecimal.class,t.business,location,t.product));
    }
    @Test void operatingExpenseNeverCreatesStockAndReportsStayBranchScoped(){
        var t=tenant();var expense=new CommerceFinanceService.Expense(key(),t.branch,t.bank,n("25"),"Electricity");finance.expense(t.actor,t.business,expense);finance.expense(t.actor,t.business,expense);amount("25",balance(t,"EXPENSE"));amount("10",stock(t));
        UUID accountant=config.createStaff(t.actor,t.business,key()+"@example.test","Accountant","isolated accountant passphrase",List.of("ACCOUNTANT"),List.of(t.branch));UUID branch=(UUID)foundation.addBranch(t.actor,t.business,"Private").get("id");assertThrows(ApiException.class,()->finance.reports(accountant,t.business,branch,LocalDate.now().minusDays(1),LocalDate.now()));
        assertFalse(((List<?>)finance.reports(accountant,t.business,null,LocalDate.now().minusDays(1),LocalDate.now()).get("accounts")).isEmpty());
    }
    @Test void concurrentSettlementAndRetryAfterBalanceChangesNeverAllocateTwice() throws Exception {
        var t=tenant();UUID doc=post(t,"SALE","5");var first=settlement(t,doc,"60",key());var pool=Executors.newFixedThreadPool(2);
        try{var gate=new CountDownLatch(1);Callable<Object> pay=()->{gate.await();return trading.settle(t.actor,t.business,first).get("id");};var a=pool.submit(pay);var b=pool.submit(pay);gate.countDown();assertEquals(a.get(),b.get());
            trading.settle(t.actor,t.business,settlement(t,doc,"40",key()));assertEquals(true,trading.settle(t.actor,t.business,first).get("replayed"));amount("0",balance(t,"RECEIVABLE"));amount("100",balance(t,"CASH_BANK"));assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM commerce_payments WHERE document_id=?",Integer.class,doc));
        }finally{pool.shutdownNow();}
    }
    @Test void recoverableAndNonrecoverablePurchaseTaxAreCapitalizedCorrectly(){
        for(boolean recoverable:List.of(true,false)){
            var t=tenant();catalog.updateProduct(t.actor,t.business,t.product,new CommerceCatalogService.ProductUpdate("Product","Description",null,null,n("10"),n("20"),null,n("5"),n("18"),false,recoverable,n("2"),true));
            UUID doc=post(t,"PURCHASE","2");trading.receive(t.actor,t.business,new CommerceTradingService.Receive(key(),doc,line(t,doc),n("2")));
            amount("-28",balance(t,"PAYABLE"));amount(recoverable?"4":"0",balance(t,"INPUT_TAX"));amount(recoverable?"124":"128",balance(t,"INVENTORY"));amount("0",balance(t,"GOODS_CLEARING"));
            trading.settle(t.actor,t.business,settlement(t,doc,"28",key()));amount("0",balance(t,"PAYABLE"));amount("0",balance(t,"EXPENSE"));
        }
    }
    @Test void branchPricesMinimumAndReceiptSnapshotsSurviveConfigurationChanges(){
        var t=tenant();catalog.updateProduct(t.actor,t.business,t.product,new CommerceCatalogService.ProductUpdate("Original","Description",null,null,n("10"),n("20"),null,n("15"),n("0"),false,true,n("2"),true));
        catalog.branchPrice(t.actor,t.business,t.product,t.branch,n("25"),OffsetDateTime.now().minusSeconds(1));UUID doc=post(t,"SALE","1");amount("-25",balance(t,"SALES"));
        assertThrows(ApiException.class,()->trading.invoice(t.actor,t.business,new CommerceTradingService.Invoice(key(),"SALE",t.location,t.customer,"RWF",null,null,false,List.of(new CommerceTradingService.Line(t.product,null,n("1"),n("14"),null,"Discount")),List.of())));
        config.branch(t.actor,t.business,t.branch,"Renamed",null,true);db.update("UPDATE commerce_products SET name='Renamed',retail_price=99 WHERE id=?",t.product);
        var detail=trading.detail(t.actor,t.business,doc);assertEquals("Main",detail.get("branch_name"));assertEquals("Original",((Map<?,?>)((List<?>)detail.get("lines")).getFirst()).get("description"));
        var request=key();trading.print(t.actor,t.business,doc,request);trading.print(t.actor,t.business,doc,request);assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM commerce_financial_commands WHERE business_id=? AND kind='RECEIPT_PRINT'",Integer.class,t.business));
        as(t.actor);assertInstanceOf(String.class,((Map<?,?>)controller.detail(t.business,doc)).get("total"));
    }
    @Test void storekeepersCanReceiveWithoutSeeingCostsAndAuditorsCannotReadPurchases(){
        var t=tenant();UUID keeper=config.createStaff(t.actor,t.business,key()+"@example.test","Keeper","isolated keeper passphrase",List.of("STOREKEEPER"),List.of(t.branch));UUID doc=post(t,"PURCHASE","1");
        var detail=trading.detail(keeper,t.business,doc);assertFalse(detail.containsKey("total"));var line=(Map<?,?>)((List<?>)detail.get("lines")).getFirst();for(String key:List.of("unit_price","line_total","inventory_value","net","base_net"))assertFalse(line.containsKey(key));
        trading.receive(keeper,t.business,new CommerceTradingService.Receive(key(),doc,line(t,doc),n("1")));amount("11",stock(t));
        UUID viewer=config.createStaff(t.actor,t.business,key()+"@example.test","Viewer","isolated viewer passphrase",List.of("VIEWER"),List.of(t.branch));assertThrows(ApiException.class,()->trading.detail(viewer,t.business,doc));assertTrue(trading.history(viewer,t.business,t.branch).isEmpty());
    }
    @Test void customerLimitChangesAreAuditedAndUnapprovedCreditRollsBack(){
        var t=tenant();trading.updateParty(t.actor,t.business,t.customer,new CommerceTradingService.PartyUpdate(key(),"Customer",null,n("10"),true));
        assertThrows(ApiException.class,()->post(t,"SALE","1"));amount("10",stock(t));amount("0",balance(t,"SALES"));
        trading.updateParty(t.actor,t.business,t.customer,new CommerceTradingService.PartyUpdate(key(),"Customer",null,n("100"),true));post(t,"SALE","1");amount("9",stock(t));
        assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM commerce_financial_commands WHERE business_id=? AND kind='PARTY_UPDATE'",Integer.class,t.business));
    }
    @Test void moduleAdministrationDoesNotGrantOperationalAccess(){
        var t=tenant();UUID admin=user();db.update("INSERT INTO platform_administrators(user_id) VALUES(?)",admin);as(t.actor);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->provisioning.commerceModules(t.business,Set.of("CORE","INVENTORY")));
        as(admin);provisioning.commerceModules(t.business,Set.of("CORE","INVENTORY"));assertThrows(ApiException.class,()->post(t,"SALE","1"));
        provisioning.commerceModules(t.business,Set.of("CORE","INVENTORY","ACCOUNTING","RETAIL"));post(t,"SALE","1");assertThrows(ApiException.class,()->trading.history(admin,t.business,t.branch));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM commerce_memberships WHERE business_id=? AND user_id=?",Integer.class,t.business,admin));
    }
    @Test void currencyRoundingCannotWeakenTheAuthorizedMinimum(){
        var t=tenant();config.configureCurrency(t.actor,t.business,"USD",true);config.rate(t.actor,t.business,"USD",n("1000"),OffsetDateTime.now().minusDays(1),"Test rate");
        catalog.updateProduct(t.actor,t.business,t.product,new CommerceCatalogService.ProductUpdate("Product",null,null,null,n("10"),n("14"),null,n("14"),n("0"),false,true,n("0"),true));
        var below=new CommerceTradingService.Invoice(key(),"SALE",t.location,t.customer,"USD",null,null,false,List.of(new CommerceTradingService.Line(t.product,null,n("1"),n("0.01"),null,"Below base minimum")),List.of());
        assertThrows(ApiException.class,()->trading.invoice(t.actor,t.business,below));amount("10",stock(t));
        var standard=new CommerceTradingService.Invoice(key(),"SALE",t.location,t.customer,"USD",null,null,false,List.of(new CommerceTradingService.Line(t.product,null,n("1"),null,null,null)),List.of());
        UUID doc=(UUID)trading.invoice(t.actor,t.business,standard).get("id");amount("0.02",db.queryForObject("SELECT total FROM commerce_documents WHERE id=?",BigDecimal.class,doc));amount("-20",balance(t,"SALES"));
    }
}
