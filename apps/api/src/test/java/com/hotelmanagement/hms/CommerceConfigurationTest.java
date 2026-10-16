package com.hotelmanagement.hms;

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
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"hms.commerce.enabled=true","hms.commerce.self-registration-enabled=false","hms.security.jwt.secret=commerce-isolated-test-secret-at-least-32-bytes","logging.level.root=WARN","management.otlp.metrics.export.enabled=false"})
class CommerceConfigurationTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){IntegrationServices.configure(r);}
    @Autowired JdbcTemplate db;
    @Autowired UserAccountService users;
    @Autowired CommerceConfigurationService config;
    @Autowired CommerceFoundationService foundation;
    @Autowired CommerceCatalogService catalog;
    @Autowired CommerceOpeningStockService opening;
    @Autowired CommerceStockService stock;
    @Autowired PlatformTransactionManager transactions;
    record Tenant(UUID actor,UUID business,UUID branch,UUID location,UUID product) {}
    UUID user(){return users.createAccount(UUID.randomUUID()+"@example.test","commerce isolated passphrase","Employee",null,"en").getId();}
    UUID emptyBusiness(UUID owner){
        UUID business=UUID.randomUUID();
        db.update("INSERT INTO commerce_businesses(id,name,base_currency) VALUES(?,'Currency setup','RWF')",business);
        db.update("INSERT INTO commerce_memberships(business_id,user_id,role) VALUES(?,?,'OWNER')",business,owner);
        db.update("INSERT INTO commerce_entitlements(business_id,module,enabled) VALUES(?,'CORE',true)",business);
        return business;
    }
    @Test void ownerCanChooseBaseBeforeUseAndRetryWithoutDuplicateAudit(){
        UUID actor=user(),business=emptyBusiness(actor);
        assertEquals(false,config.baseCurrencySettings(actor,business).get("locked"));
        assertThrows(ApiException.class,()->config.setBaseCurrency(actor,business,"BAD"));
        config.configureCurrency(actor,business,"USD",false);
        assertEquals("USD",config.setBaseCurrency(actor,business," usd ").get("currency"));
        config.setBaseCurrency(actor,business,"USD");
        assertEquals(true,db.queryForObject("SELECT active FROM commerce_currencies WHERE business_id=? AND currency='USD'",Boolean.class,business));
        assertEquals(1,db.queryForObject("SELECT count(*) FROM commerce_configuration_audit WHERE business_id=? AND action='BASE_CURRENCY_CHANGED'",Integer.class,business));
        var quote=config.quote(actor,business,"USD",new BigDecimal("1.23"),java.time.OffsetDateTime.now());
        assertEquals("USD",quote.baseCurrency());amount("1",quote.rateToBase());amount("1.23",quote.baseAmount());
    }
    @Test void baseCurrencyRejectsOtherTenantsAndStaff(){
        var t=tenant();UUID other=user(),business=emptyBusiness(t.actor);
        assertThrows(ApiException.class,()->config.baseCurrencySettings(other,business));
        assertThrows(ApiException.class,()->config.setBaseCurrency(other,business,"USD"));
        db.update("INSERT INTO commerce_memberships(business_id,user_id,role) VALUES(?,?,'MANAGER')",business,other);
        assertThrows(ApiException.class,()->config.setBaseCurrency(other,business,"USD"));
        assertEquals("RWF",config.baseCurrencySettings(t.actor,business).get("currency"));
    }
    @Test void existingProductsAndExchangeRatesLockBaseCurrency(){
        var t=tenant();
        assertEquals(true,config.baseCurrencySettings(t.actor,t.business).get("locked"));
        assertThrows(ApiException.class,()->config.setBaseCurrency(t.actor,t.business,"USD"));
        assertEquals("RWF",config.setBaseCurrency(t.actor,t.business,"RWF").get("currency"));
        UUID business=emptyBusiness(t.actor);
        config.configureCurrency(t.actor,business,"USD",true);
        config.rate(t.actor,business,"USD",new BigDecimal("1400"),java.time.OffsetDateTime.now(),"Test rate");
        assertThrows(ApiException.class,()->config.setBaseCurrency(t.actor,business,"USD"));
        assertEquals("RWF",config.baseCurrencySettings(t.actor,business).get("currency"));
    }
    Tenant tenant(){
        UUID actor=user(),business=UUID.randomUUID();
        db.update("INSERT INTO commerce_businesses(id,name,base_currency) VALUES(?,'Commerce test','RWF')",business);
        db.update("INSERT INTO commerce_memberships(business_id,user_id,role) VALUES(?,?,'OWNER')",business,actor);
        for(String module:List.of("CORE","INVENTORY"))db.update("INSERT INTO commerce_entitlements(business_id,module,enabled) VALUES(?,?,true)",business,module);
        UUID branch=(UUID)foundation.addBranch(actor,business,"Main").get("id");
        UUID location=(UUID)foundation.addLocation(actor,business,branch,"Warehouse","WAREHOUSE").get("id");
        UUID product=(UUID)catalog.addProduct(actor,business,null,"SKU",null,"Product","unit",BigDecimal.TEN,new BigDecimal("15"),new BigDecimal("12")).get("id");
        return new Tenant(actor,business,branch,location,product);
    }
    UUID category(Tenant t,String name,UUID parent){return (UUID)catalog.addCategory(t.actor,t.business,name,parent).get("id");}
    UUID employee(Tenant t,String role){return config.createStaff(t.actor,t.business,UUID.randomUUID()+"@example.test","Employee","employee isolated passphrase",List.of(role),List.of(t.branch));}
    UUID location(Tenant t,String name){return (UUID)foundation.addLocation(t.actor,t.business,t.branch,name,"WAREHOUSE").get("id");}
    void opening(Tenant t,String amount){opening.opening(t.actor,t.business,t.location,t.product,UUID.randomUUID(),new BigDecimal(amount));}
    CommerceStockService.Command command(Tenant t,UUID destination,String qty,UUID key){return new CommerceStockService.Command(key,t.location,destination,t.product,new BigDecimal(qty),"Verified count");}
    BigDecimal balance(Tenant t,UUID location){var rows=db.queryForList("SELECT quantity FROM commerce_stock_balances WHERE business_id=? AND location_id=? AND product_id=?",BigDecimal.class,t.business,location,t.product);return rows.isEmpty()?BigDecimal.ZERO:rows.getFirst();}
    void amount(String expected,BigDecimal actual){assertEquals(0,new BigDecimal(expected).compareTo(actual));}
    @Test void categoriesSupportTopLevelEditAndRejectCyclesAndInUseDeactivation(){
        var t=tenant();var parent=category(t,"Parent",null);var child=category(t,"Child",parent);
        assertThrows(ApiException.class,()->config.category(t.actor,t.business,parent,"Parent",child,true));
        assertThrows(ApiException.class,()->config.category(t.actor,t.business,parent,"Parent",null,false));
        config.category(t.actor,t.business,child,"Renamed",null,true);
        config.category(t.actor,t.business,parent,"Parent",null,false);
        assertEquals("Renamed",db.queryForObject("SELECT name FROM commerce_categories WHERE id=?",String.class,child));
        assertEquals(2,config.history(t.actor,t.business).size());
    }
    @Test void foreignCategoryAndOwnerMutationAreRejected(){var t=tenant();var other=tenant();var category=category(t,"Category",null);
        assertThrows(ApiException.class,()->config.category(other.actor,t.business,category,"Stolen",null,true));
        assertThrows(ApiException.class,()->config.category(t.actor,t.business,category,"Category",category(other,"Other",null),true));
    }
    @Test void baseCurrencyQuoteUsesIsoPrecisionAndRatesAreDatedImmutable(){
        var t=tenant();config.configureCurrency(t.actor,t.business,"USD",true);
        var first=OffsetDateTime.parse("2025-01-01T00:00:00Z");
        UUID rate=config.rate(t.actor,t.business,"USD",new BigDecimal("1400.5"),first,"Treasury quote");
        var snapshot=config.quote(t.actor,t.business,"USD",new BigDecimal("10.01"),first.plusDays(1));
        assertEquals(rate,snapshot.rateId());amount("14019",snapshot.baseAmount());
        config.rate(t.actor,t.business,"USD",new BigDecimal("1500"),first.plusDays(2),"New quote");
        assertEquals(snapshot,config.quote(t.actor,t.business,"USD",new BigDecimal("10.01"),first.plusDays(1)));
        assertThrows(ApiException.class,()->config.quote(t.actor,t.business,"USD",new BigDecimal("1.001"),first));
        assertThrows(ApiException.class,()->config.quote(t.actor,t.business,"USD",BigDecimal.ONE,first.minusDays(1)));
        assertThrows(ApiException.class,()->config.configureCurrency(t.actor,t.business,"RWF",false));
        assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("DELETE FROM commerce_exchange_rates WHERE id=?",rate));
        amount("100",config.quote(t.actor,t.business,"RWF",new BigDecimal("100"),first).baseAmount());
    }
    @Test void employeeIsHashedTemporaryAndCannotSeeCostsOrManageCatalog(){
        var t=tenant();UUID cashier=employee(t,"CASHIER");
        assertTrue(db.queryForObject("SELECT must_change_password FROM users WHERE id=?",Boolean.class,cashier));
        assertNotEquals("employee isolated passphrase",db.queryForObject("SELECT password_hash FROM users WHERE id=?",String.class,cashier));
        assertFalse(catalog.products(cashier,t.business).getFirst().containsKey("purchase_cost"));
        assertTrue(catalog.products(t.actor,t.business).getFirst().containsKey("purchase_cost"));
        assertThrows(ApiException.class,()->catalog.addCategory(cashier,t.business,"Forbidden",null));
        assertThrows(ApiException.class,()->config.configureCurrency(cashier,t.business,"USD",true));
        assertThrows(ApiException.class,()->config.staff(cashier,t.business));
        assertThrows(ApiException.class,()->opening.opening(cashier,t.business,t.location,t.product,UUID.randomUUID(),BigDecimal.ONE));
    }
    @Test void staffCannotReadUnassignedBranchAndRevocationTakesEffect(){
        var t=tenant();UUID cashier=employee(t,"CASHIER");
        var branch=(UUID)foundation.addBranch(t.actor,t.business,"Other").get("id");
        var location=(UUID)foundation.addLocation(t.actor,t.business,branch,"Other warehouse","WAREHOUSE").get("id");
        assertEquals(1,foundation.branches(cashier,t.business).size());
        assertThrows(ApiException.class,()->foundation.locations(cashier,t.business,branch));
        assertThrows(ApiException.class,()->opening.balances(cashier,t.business,location));
        config.location(t.actor,t.business,t.location,"Warehouse",false);
        config.branch(t.actor,t.business,t.branch,"Main",null,false);
        config.updateStaff(t.actor,t.business,cashier,List.of("CASHIER"),false,List.of(t.branch));
        assertThrows(ApiException.class,()->catalog.products(cashier,t.business));
    }
    @Test void noImplicitSuperadminOrCrossTenantAccess(){
        var t=tenant();var superadmin=user();
        db.update("INSERT INTO platform_administrators(user_id) VALUES(?)",superadmin);
        assertThrows(ApiException.class,()->catalog.products(superadmin,t.business));
        assertThrows(ApiException.class,()->config.history(superadmin,t.business));
        assertThrows(ApiException.class,()->stock.ledger(superadmin,t.business,t.location));
        assertThrows(ApiException.class,()->foundation.create(t.actor,"Self registration","RWF"));
    }
    @Test void locationWithStockAndBranchWithActiveLocationsCannotBeDeactivated(){
        var t=tenant();opening(t,"10");
        assertThrows(ApiException.class,()->config.location(t.actor,t.business,t.location,"Warehouse",false));
        assertThrows(ApiException.class,()->config.branch(t.actor,t.business,t.branch,"Main",null,false));
        config.branch(t.actor,t.business,t.branch,"Renamed","0123",true);
        assertEquals("0123",foundation.branches(t.actor,t.business).getFirst().get("contact"));
        var empty=location(t,"Empty");config.location(t.actor,t.business,empty,"Closed",false);
        assertThrows(ApiException.class,()->opening.opening(t.actor,t.business,empty,t.product,UUID.randomUUID(),BigDecimal.ONE));
        assertTrue(stock.ledger(t.actor,t.business,empty).isEmpty());
    }
    @Test void transferReplayDoesNotDuplicateStockAndRejectsChangedDestination(){
        var t=tenant();opening(t,"10");var destination=location(t,"Destination");var another=location(t,"Another");var key=UUID.randomUUID();
        stock.post(t.actor,t.business,command(t,destination,"3",key));
        assertEquals(true,stock.post(t.actor,t.business,command(t,destination,"3.000",key)).get("replayed"));
        assertThrows(ApiException.class,()->stock.post(t.actor,t.business,command(t,another,"3",key)));
        assertThrows(ApiException.class,()->stock.post(t.actor,t.business,command(t,destination,"4",key)));
        amount("7",balance(t,t.location));amount("3",balance(t,destination));amount("0",balance(t,another));
        assertEquals(2,db.queryForObject("SELECT count(*) FROM commerce_stock_movements WHERE business_id=? AND command_id IS NOT NULL",Integer.class,t.business));
    }
    @Test void insufficientTransferAndTransactionFailureRollBackEveryLeg(){
        var t=tenant();opening(t,"10");var destination=location(t,"Destination");var key=UUID.randomUUID();
        assertThrows(ApiException.class,()->stock.post(t.actor,t.business,command(t,destination,"11",key)));
        assertEquals(0,db.queryForObject("SELECT count(*) FROM commerce_stock_commands WHERE business_id=?",Integer.class,t.business));
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).executeWithoutResult(s->{stock.post(t.actor,t.business,command(t,destination,"3",key));throw new IllegalStateException("Forced rollback");}));
        amount("10",balance(t,t.location));amount("0",balance(t,destination));
        assertEquals(0,db.queryForObject("SELECT count(*) FROM commerce_stock_commands WHERE business_id=?",Integer.class,t.business));
    }
    void race(Runnable action) throws Exception {
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try {var tasks=new ArrayList<Future<?>>();for(int i=0;i<2;i++)tasks.add(pool.submit(()->{ready.countDown();try{if(!start.await(30,TimeUnit.SECONDS))throw new AssertionError("Timeout");action.run();}catch(InterruptedException e){throw new AssertionError(e);}}));assertTrue(ready.await(30,TimeUnit.SECONDS));start.countDown();for(var task:tasks)task.get(60,TimeUnit.SECONDS);}
        finally{start.countDown();pool.shutdownNow();}
    }
    @Test void concurrentReplayAndIndependentTransfersCannotOverdraw() throws Exception {
        var t=tenant();opening(t,"10");var destination=location(t,"Destination");var key=UUID.randomUUID();
        race(()->stock.post(t.actor,t.business,command(t,destination,"3",key)));
        amount("7",balance(t,t.location));amount("3",balance(t,destination));
        var rejected=new java.util.concurrent.atomic.AtomicInteger();
        race(()->{try{stock.post(t.actor,t.business,command(t,destination,"5",UUID.randomUUID()));}catch(ApiException e){assertEquals("INSUFFICIENT_STOCK",e.code());rejected.incrementAndGet();}});
        assertEquals(1,rejected.get());amount("2",balance(t,t.location));amount("8",balance(t,destination));
    }
    @Test void storekeeperCanAdjustAssignedStockButCannotTransferToUnassignedBranch(){
        var t=tenant();var keeper=employee(t,"STOREKEEPER");
        stock.post(keeper,t.business,command(t,null,"5",UUID.randomUUID()));amount("5",balance(t,t.location));
        var branch=(UUID)foundation.addBranch(t.actor,t.business,"Other").get("id");
        var location=(UUID)foundation.addLocation(t.actor,t.business,branch,"Other warehouse","WAREHOUSE").get("id");
        assertThrows(ApiException.class,()->stock.post(keeper,t.business,command(t,location,"1",UUID.randomUUID())));
        assertThrows(ApiException.class,()->catalog.addProduct(keeper,t.business,null,"OTHER",null,"Other","unit",BigDecimal.ONE,BigDecimal.TEN,BigDecimal.TEN));
        assertThrows(ApiException.class,()->stock.post(employee(t,"CASHIER"),t.business,command(t,null,"5",UUID.randomUUID())));
    }
    @Test void foreignBranchAssignmentRollsBackNewIdentity(){
        var t=tenant();var other=tenant();String email=UUID.randomUUID()+"@example.test";
        assertThrows(ApiException.class,()->config.createStaff(t.actor,t.business,email,"Employee","employee isolated passphrase",List.of("CASHIER"),List.of(other.branch)));
        assertEquals(0,db.queryForObject("SELECT count(*) FROM users WHERE email=?",Integer.class,email));
        assertThrows(ApiException.class,()->config.updateStaff(t.actor,t.business,t.actor,List.of("MANAGER"),true,List.of(t.branch)));
    }
    @Test void concurrentCategoryEditsCannotCreateCycle() throws Exception {
        var t=tenant();var a=category(t,"A",null);var b=category(t,"B",null);
        var sequence=new java.util.concurrent.atomic.AtomicInteger();var rejected=new java.util.concurrent.atomic.AtomicInteger();
        race(()->{boolean first=sequence.getAndIncrement()==0;try{config.category(t.actor,t.business,first?a:b,first?"A":"B",first?b:a,true);}catch(ApiException e){rejected.incrementAndGet();}});
        assertEquals(1,rejected.get());
    }
    @Test void productTrackingFiltersMovementsAndKeepsTenantBoundaries(){
        var t=tenant();var other=tenant();
        UUID second=(UUID)catalog.addProduct(t.actor,t.business,null,"SECOND",null,"Second","unit",BigDecimal.ONE,BigDecimal.TEN,null).get("id");
        stock.post(t.actor,t.business,command(t,null,"5",UUID.randomUUID()));
        stock.post(t.actor,t.business,new CommerceStockService.Command(UUID.randomUUID(),t.location,null,second,BigDecimal.ONE,"Other product"));
        var movements=stock.ledger(t.actor,t.business,t.location,t.product);
        assertEquals(1,movements.size());assertEquals(t.product,movements.getFirst().get("product_id"));
        assertEquals(2,stock.ledger(t.actor,t.business,t.location).size());
        assertTrue(stock.ledger(t.actor,t.business,t.location,other.product).isEmpty());
        assertThrows(ApiException.class,()->stock.ledger(other.actor,t.business,t.location,t.product));
    }
    @Test void deactivationPreservesStockAndRejectsUnauthorizedChanges(){
        var t=tenant();var other=tenant();
        stock.post(t.actor,t.business,command(t,null,"5",UUID.randomUUID()));
        var before=db.queryForMap("SELECT * FROM commerce_products WHERE id=?",t.product);
        assertThrows(ApiException.class,()->catalog.deactivateProduct(other.actor,t.business,t.product));
        assertThrows(ApiException.class,()->catalog.deactivateProduct(t.actor,t.business,other.product));
        assertThrows(ApiException.class,()->catalog.deactivateProduct(employee(t,"CASHIER"),t.business,t.product));
        catalog.deactivateProduct(t.actor,t.business,t.product);
        var after=db.queryForMap("SELECT * FROM commerce_products WHERE id=?",t.product);
        before.put("active",false);assertEquals(before,after);
        assertEquals(0,new BigDecimal("5").compareTo(db.queryForObject("SELECT quantity FROM commerce_stock_balances WHERE business_id=? AND location_id=? AND product_id=?",BigDecimal.class,t.business,t.location,t.product)));
        int audits=db.queryForObject("SELECT count(*) FROM commerce_configuration_audit WHERE business_id=? AND entity_id=?",Integer.class,t.business,t.product);
        catalog.deactivateProduct(t.actor,t.business,t.product);
        assertEquals(audits,db.queryForObject("SELECT count(*) FROM commerce_configuration_audit WHERE business_id=? AND entity_id=?",Integer.class,t.business,t.product));
    }
    @Test void numericBoundsAndImmutableCommandsAreEnforced(){
        var t=tenant();
        assertEquals("INVALID_STOCK_COMMAND",assertThrows(ApiException.class,()->stock.post(t.actor,t.business,command(t,null,"1E20",UUID.randomUUID()))).code());
        config.configureCurrency(t.actor,t.business,"USD",true);
        assertThrows(ApiException.class,()->config.rate(t.actor,t.business,"USD",new BigDecimal("1E20"),OffsetDateTime.now(),"Too large"));
        stock.post(t.actor,t.business,command(t,null,"2",UUID.randomUUID()));
        assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("DELETE FROM commerce_stock_commands WHERE business_id=?",t.business));
        assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE commerce_stock_movements SET quantity_delta=3 WHERE business_id=?",t.business));
        assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("DELETE FROM commerce_configuration_audit WHERE business_id=?",t.business));
    }
}
