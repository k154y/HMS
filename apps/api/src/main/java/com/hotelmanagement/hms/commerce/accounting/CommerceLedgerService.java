package com.hotelmanagement.hms.commerce.accounting;

import com.hotelmanagement.hms.commerce.catalog.CommerceCatalogAccess;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.math.*;
import java.util.*;

/** Shared posting primitives. Callers hold the business lock and authorize the operation. */
@Service
@Transactional
public class CommerceLedgerService {
    private final JdbcTemplate db;
    private final CommerceCatalogAccess access;
    private final JsonMapper json;
    public CommerceLedgerService(JdbcTemplate db,CommerceCatalogAccess access,JsonMapper json){this.db=db;this.access=access;this.json=json;}
    public record Command(UUID id,boolean replayed) {}
    public record Entry(UUID branch,String account,BigDecimal debit,BigDecimal credit,UUID paymentAccount,UUID party) {}
    public static Entry debit(UUID branch,String account,BigDecimal amount){return new Entry(branch,account,amount,BigDecimal.ZERO,null,null);}
    public static Entry credit(UUID branch,String account,BigDecimal amount){return new Entry(branch,account,BigDecimal.ZERO,amount,null,null);}
    public void lock(UUID business){db.queryForObject("SELECT id FROM commerce_businesses WHERE id=? FOR UPDATE",UUID.class,business);}
    public String base(UUID business){return db.queryForObject("SELECT base_currency FROM commerce_businesses WHERE id=?",String.class,business);}
    public int precision(String currency){int n=Currency.getInstance(currency).getDefaultFractionDigits();if(n<0||n>4)throw invalid("Unsupported currency.");return n;}
    public BigDecimal rounded(UUID business,BigDecimal amount){return amount.setScale(precision(base(business)),RoundingMode.HALF_UP);}
    public static ApiException invalid(String message){return new ApiException(400,"INVALID_FINANCIAL_DOCUMENT",message);}
    public static BigDecimal decimal(Object value){return value==null?BigDecimal.ZERO:(BigDecimal)value;}
    public boolean active(UUID business){return Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM commerce_accounting_state WHERE business_id=?)",Boolean.class,business));}
    public void requireActive(UUID business){if(!active(business))throw new ApiException(409,"ACCOUNTING_NOT_INITIALIZED","The owner must approve opening inventory valuation first.");}
    public Command command(UUID actor,UUID business,UUID request,String kind,Object payload) {
        if(request==null)throw invalid("A stable request identifier is required.");
        String encoded=json.writeValueAsString(payload);
        var existing=db.queryForList("SELECT id,actor_id,kind,payload=CAST(? AS jsonb) AS same FROM commerce_financial_commands WHERE business_id=? AND request_id=?",encoded,business,request);
        if(!existing.isEmpty()) {
            var row=existing.getFirst();
            if(!actor.equals(row.get("actor_id"))||!kind.equals(row.get("kind"))||!Boolean.TRUE.equals(row.get("same")))
                throw new ApiException(409,"FINANCIAL_REQUEST_CONFLICT","This request identifier was used for a different transaction.");
            return new Command((UUID)row.get("id"),true);
        }
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO commerce_financial_commands(id,business_id,request_id,kind,actor_id,payload) VALUES(?,?,?,?,?,CAST(? AS jsonb))",id,business,request,kind,actor,encoded);
        return new Command(id,false);
    }
    public UUID journal(UUID actor,UUID business,String kind,UUID source,String description,List<Entry> entries) {
        // Validate before inserting and again with deferred database constraints at commit.
        var totals=new HashMap<UUID,BigDecimal>();
        for(var entry:entries) {
            if(entry.debit().signum()<0||entry.credit().signum()<0)throw invalid("Journal amounts must not be negative.");
            totals.merge(entry.branch(),entry.debit().subtract(entry.credit()),BigDecimal::add);
        }
        if(totals.values().stream().anyMatch(v->v.signum()!=0))throw new IllegalStateException("Unbalanced commerce posting.");
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO commerce_journals(id,business_id,source_kind,source_id,description,created_by) VALUES(?,?,?,?,?,?)",id,business,kind,source,description,actor);
        for(var e:entries)if(e.debit().signum()!=0||e.credit().signum()!=0)
            db.update("INSERT INTO commerce_journal_lines(id,business_id,journal_id,branch_id,account,debit,credit,payment_account_id,party_id) VALUES(?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),business,id,e.branch(),e.account(),e.debit(),e.credit(),e.paymentAccount(),e.party());
        return id;
    }
    public Map<String,Object> activate(UUID actor,UUID business,UUID request) {
        access.owner(actor,business);access.require(actor,business,"ACCOUNTING",true);lock(business);access.owner(actor,business);access.require(actor,business,"ACCOUNTING",true);
        var cmd=command(actor,business,request,"ACTIVATE_ACCOUNTING",Map.of("approval","CURRENT_CATALOG_OPENING_COSTS"));
        if(cmd.replayed())return Map.of("id",cmd.id(),"replayed",true);
        if(active(business))throw new ApiException(409,"ACCOUNTING_ALREADY_ACTIVE","Accounting has already been activated.");
        var entries=new ArrayList<Entry>();
        for(var row:db.queryForList("""
          SELECT s.location_id,s.product_id,s.quantity,p.purchase_cost,l.branch_id FROM commerce_stock_balances s
          JOIN commerce_products p ON p.business_id=s.business_id AND p.id=s.product_id
          JOIN commerce_stock_locations l ON l.business_id=s.business_id AND l.id=s.location_id WHERE s.business_id=?
          """,business)) {
            var value=rounded(business,decimal(row.get("quantity")).multiply(decimal(row.get("purchase_cost"))));
            db.update("INSERT INTO commerce_opening_values(business_id,activation_id,location_id,product_id,quantity,unit_cost,inventory_value) VALUES(?,?,?,?,?,?,?)",business,cmd.id(),row.get("location_id"),row.get("product_id"),row.get("quantity"),row.get("purchase_cost"),value);
            db.update("UPDATE commerce_stock_balances SET inventory_value=? WHERE business_id=? AND location_id=? AND product_id=?",value,business,row.get("location_id"),row.get("product_id"));
            entries.add(debit((UUID)row.get("branch_id"),"INVENTORY",value));entries.add(credit((UUID)row.get("branch_id"),"OPENING_EQUITY",value));
        }
        journal(actor,business,"ACTIVATE_ACCOUNTING",cmd.id(),"Owner-approved opening valuation at current catalog purchase costs",entries);
        db.update("INSERT INTO commerce_accounting_state(business_id,activation_id,created_by) VALUES(?,?,?)",business,cmd.id(),actor);
        return Map.of("id",cmd.id(),"replayed",false);
    }
    /** Called before quantity changes. Returns positive value added/removed; never recalculates prior movements. */
    public BigDecimal value(UUID business,UUID location,UUID product,BigDecimal delta,BigDecimal incomingValue) {
        if(!active(business))return BigDecimal.ZERO;
        db.update("INSERT INTO commerce_stock_balances(business_id,location_id,product_id,quantity) VALUES(?,?,?,0) ON CONFLICT DO NOTHING",business,location,product);
        var balance=db.queryForMap("SELECT quantity,inventory_value FROM commerce_stock_balances WHERE business_id=? AND location_id=? AND product_id=? FOR UPDATE",business,location,product);
        BigDecimal quantity=decimal(balance.get("quantity")),carrying=decimal(balance.get("inventory_value")),amount;
        if(delta.signum()<0) {
            if(quantity.add(delta).signum()<0)throw new ApiException(409,"INSUFFICIENT_STOCK","Insufficient stock at the selected location.");
            amount=quantity.add(delta).signum()==0?carrying:rounded(business,carrying.multiply(delta.abs()).divide(quantity,12,RoundingMode.HALF_UP));
            db.update("UPDATE commerce_stock_balances SET inventory_value=inventory_value-? WHERE business_id=? AND location_id=? AND product_id=?",amount,business,location,product);
        } else {
            amount=incomingValue!=null?rounded(business,incomingValue):rounded(business,delta.multiply(db.queryForObject("SELECT purchase_cost FROM commerce_products WHERE business_id=? AND id=?",BigDecimal.class,business,product)));
            if(amount.signum()<0)throw invalid("Inventory value must not be negative.");
            db.update("UPDATE commerce_stock_balances SET inventory_value=inventory_value+? WHERE business_id=? AND location_id=? AND product_id=?",amount,business,location,product);
        }
        return amount;
    }
    public void stockJournal(UUID actor,UUID business,UUID location,String kind,UUID source,BigDecimal delta,BigDecimal amount) {
        if(!active(business))return;
        UUID branch=db.queryForObject("SELECT branch_id FROM commerce_stock_locations WHERE business_id=? AND id=?",UUID.class,business,location);
        String offset=kind.equals("OPENING_STOCK")?"OPENING_EQUITY":kind.startsWith("TRANSFER")?"INTERBRANCH":delta.signum()<0?"STOCK_LOSS":"STOCK_GAIN";
        journal(actor,business,kind,source,"Inventory "+kind,delta.signum()>0?List.of(debit(branch,"INVENTORY",amount),credit(branch,offset,amount)):List.of(debit(branch,offset,amount),credit(branch,"INVENTORY",amount)));
    }
}
