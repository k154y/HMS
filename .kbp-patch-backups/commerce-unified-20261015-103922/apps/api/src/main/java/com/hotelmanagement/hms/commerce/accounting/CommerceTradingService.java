package com.hotelmanagement.hms.commerce.accounting;

import com.hotelmanagement.hms.commerce.catalog.CommerceCatalogAccess;
import com.hotelmanagement.hms.commerce.foundation.CommerceConfigurationService;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.hotelmanagement.hms.commerce.accounting.CommerceLedgerService.*;

/** Immutable invoice, receipt and settlement events; all writers serialize on the business row. */
@Service
@Transactional
public class CommerceTradingService {
    private final JdbcTemplate db;
    private final CommerceCatalogAccess access;
    private final CommerceLedgerService ledger;
    private final CommerceConfigurationService currencies;
    public CommerceTradingService(JdbcTemplate db,CommerceCatalogAccess access,CommerceLedgerService ledger,CommerceConfigurationService currencies){this.db=db;this.access=access;this.ledger=ledger;this.currencies=currencies;}
    public record Line(@NotNull UUID productId,String unit,@NotNull BigDecimal quantity,BigDecimal unitPrice,BigDecimal discount,String reason) {}
    public record Tender(@NotNull UUID accountId,@NotNull BigDecimal amount,UUID shiftId,String reference) {}
    public record Invoice(UUID requestId,@NotBlank String kind,@NotNull UUID locationId,UUID partyId,@NotBlank String currency,String reference,LocalDate dueDate,boolean wholesale,@NotEmpty @Size(max=100) List<@NotNull @Valid Line> lines,@Size(max=10) List<@NotNull @Valid Tender> payments) {}
    public record Receive(@NotNull UUID requestId,@NotNull UUID documentId,@NotNull UUID lineId,@NotNull BigDecimal quantity) {}
    public record Settlement(@NotNull UUID requestId,@NotNull UUID documentId,@NotNull @Valid Tender payment,boolean refund) {}
    public record Return(@NotNull UUID requestId,@NotNull UUID documentId,@NotNull UUID lineId,@NotNull BigDecimal quantity,boolean saleable,@NotBlank String reason) {}
    public record Party(@NotNull UUID requestId,@NotBlank String kind,@NotBlank String name,String contact,@NotNull BigDecimal creditLimit) {}
    public record Account(@NotNull UUID requestId,@NotBlank String name,@NotBlank String method,@NotBlank String currency,String reference) {}
    public record PartyUpdate(@NotNull UUID requestId,@NotBlank String name,String contact,@NotNull BigDecimal creditLimit,boolean active) {}
    private record Priced(Line input,Map<String,Object> product,String unit,BigDecimal quantity,BigDecimal standard,BigDecimal price,BigDecimal discount,BigDecimal net,BigDecimal tax,BigDecimal baseNet,BigDecimal baseTax) {}
    private String role(UUID actor,UUID business,String... allowed){String role=access.role(actor,business,"ACCOUNTING");if(!Set.of(allowed).contains(role))throw new ApiException(403,"COMMERCE_FORBIDDEN","This operation requires additional permission.");return role;}
    private String write(UUID actor,UUID business,String... allowed){role(actor,business,allowed);ledger.lock(business);return role(actor,business,allowed);}
    private void text(String value,int max,boolean required){if((required&&(value==null||value.isBlank()))||(value!=null&&value.length()>max))throw invalid("Invalid text field.");}
    private BigDecimal number(BigDecimal value,int scale,boolean positive){if(value==null||value.signum()<0||(positive&&value.signum()==0)||value.stripTrailingZeros().scale()>scale||value.precision()-value.scale()>13)throw invalid("Invalid quantity or amount.");return value;}
    private BigDecimal round(String currency,BigDecimal value){return value.setScale(ledger.precision(currency),RoundingMode.HALF_UP);}
    private Map<String,Object> one(String sql,Object... args){var rows=db.queryForList(sql,args);if(rows.isEmpty())throw ApiException.notFound();return rows.getFirst();}
    private BigDecimal d(Map<String,Object> row,String key){return decimal(row.get(key));}
    private UUID id(Map<String,Object> row,String key){return (UUID)row.get(key);}
    private Map<String,Object> result(Command cmd){return Map.of("id",cmd.id(),"replayed",cmd.replayed());}

    public Map<String,Object> party(UUID actor,UUID business,Party p){
        write(actor,business,"OWNER","MANAGER","ACCOUNTANT");
        if(p==null||!Set.of("CUSTOMER","SUPPLIER").contains(p.kind()))throw invalid("Select customer or supplier.");
        text(p.name(),200,true);text(p.contact(),300,false);number(p.creditLimit(),ledger.precision(ledger.base(business)),false);
        var cmd=ledger.command(actor,business,p.requestId(),"PARTY",p);if(cmd.replayed())return result(cmd);
        db.update("INSERT INTO commerce_parties(id,business_id,kind,name,contact,credit_limit_base) VALUES(?,?,?,?,?,?)",cmd.id(),business,p.kind(),p.name().strip(),p.contact(),p.creditLimit());return result(cmd);
    }
    public List<Map<String,Object>> parties(UUID actor,UUID business){
        String r=role(actor,business,"OWNER","MANAGER","ACCOUNTANT","CASHIER","STOREKEEPER","VIEWER");
        var rows=db.queryForList("SELECT * FROM commerce_parties WHERE business_id=? AND (?<>'CASHIER' OR kind='CUSTOMER') ORDER BY name",business,r);
        if(!access.financial(actor,business))rows.forEach(row->row.remove("credit_limit_base"));return rows;
    }
    public Map<String,Object> updateParty(UUID actor,UUID business,UUID party,PartyUpdate p){
        write(actor,business,"OWNER","MANAGER","ACCOUNTANT");one("SELECT id FROM commerce_parties WHERE business_id=? AND id=?",business,party);
        text(p.name(),200,true);text(p.contact(),300,false);number(p.creditLimit(),ledger.precision(ledger.base(business)),false);
        var cmd=ledger.command(actor,business,p.requestId(),"PARTY_UPDATE",Map.of("party",party,"update",p));if(cmd.replayed())return result(cmd);
        db.update("UPDATE commerce_parties SET name=?,contact=?,credit_limit_base=?,active=? WHERE business_id=? AND id=?",p.name().strip(),p.contact(),p.creditLimit(),p.active(),business,party);return result(cmd);
    }
    public Map<String,Object> print(UUID actor,UUID business,UUID document,UUID request){
        write(actor,business,"OWNER","MANAGER","ACCOUNTANT","CASHIER","VIEWER");document(actor,business,document,false);
        return result(ledger.command(actor,business,request,"RECEIPT_PRINT",Map.of("document",document)));
    }
    public Map<String,Object> account(UUID actor,UUID business,Account p){
        write(actor,business,"OWNER");text(p.name(),200,true);text(p.reference(),200,false);
        if(!Set.of("CASH","BANK","MOBILE_MONEY","CARD").contains(p.method()))throw invalid("Invalid payment method.");
        var cmd=ledger.command(actor,business,p.requestId(),"PAYMENT_ACCOUNT",p);if(cmd.replayed())return result(cmd);
        currencies.quote(actor,business,p.currency(),BigDecimal.ZERO,OffsetDateTime.now());
        db.update("INSERT INTO commerce_payment_accounts(id,business_id,name,method,currency,reference) VALUES(?,?,?,?,?,?)",cmd.id(),business,p.name().strip(),p.method(),p.currency(),p.reference());return result(cmd);
    }
    public List<Map<String,Object>> accounts(UUID actor,UUID business){role(actor,business,"OWNER","MANAGER","ACCOUNTANT","CASHIER","VIEWER");return db.queryForList("SELECT id,name,method,currency,reference,active FROM commerce_payment_accounts WHERE business_id=? ORDER BY name",business);}
    private Map<String,Object> document(UUID actor,UUID business,UUID document,boolean active){
        var row=one("SELECT * FROM commerce_documents WHERE business_id=? AND id=?",business,document);
        access.location(actor,business,id(row,"location_id"),"ACCOUNTING",active);
        String r=access.role(actor,business,"ACCOUNTING");
        if(r.equals("CASHIER")&&(!row.get("kind").equals("SALE")||!actor.equals(row.get("created_by"))))throw ApiException.notFound();
        if(r.equals("VIEWER")&&!row.get("kind").equals("SALE"))throw ApiException.notFound();
        return row;
    }
    private List<Priced> price(UUID actor,UUID business,UUID branch,Invoice p,String role,BigDecimal rate){
        if(p.lines()==null||p.lines().isEmpty()||p.lines().size()>100)throw invalid("An invoice needs 1 to 100 lines.");
        boolean sale=p.kind().equals("SALE");var out=new ArrayList<Priced>();
        for(var input:p.lines()){
            if(input==null||input.productId()==null)throw invalid("A product is required.");
            number(input.quantity(),6,true);text(input.reason(),500,false);
            var product=one("SELECT p.*,(SELECT c.name FROM commerce_categories c WHERE c.business_id=p.business_id AND c.id=p.category_id) AS category_name FROM commerce_products p WHERE business_id=? AND id=? AND active=true",business,input.productId());
            String unit=input.unit()==null?(String)product.get("base_unit"):input.unit();
            var unitInfo=one("SELECT units_per_package,retail_package_price,wholesale_package_price FROM commerce_product_units WHERE business_id=? AND product_id=? AND unit_name=?",business,input.productId(),unit);
            BigDecimal factor=(BigDecimal)unitInfo.get("units_per_package");
            BigDecimal qty=number(input.quantity().multiply(factor),6,true);
            BigDecimal basePrice=d(product,p.wholesale()?"wholesale_price":"retail_price");
            if(p.wholesale()&&product.get("wholesale_price")==null)basePrice=d(product,"retail_price");
            var branchPrices=db.queryForList("SELECT price FROM commerce_branch_prices WHERE business_id=? AND branch_id=? AND product_id=? AND effective_at<=CURRENT_TIMESTAMP ORDER BY effective_at DESC LIMIT 1",BigDecimal.class,business,branch,input.productId());
            if(!branchPrices.isEmpty())basePrice=branchPrices.getFirst();
            // Explicit per-package prices are quoted for one full package. Fractional quantities
            // (for example 0.5 crate) multiply that same package price proportionally.
            BigDecimal packagePrice=(BigDecimal)unitInfo.get(p.wholesale()?"wholesale_package_price":"retail_package_price");
            BigDecimal effectivePrice=packagePrice==null?basePrice.multiply(factor):packagePrice;
            BigDecimal standard=round(p.currency(),effectivePrice.divide(rate,12,RoundingMode.HALF_UP));
            // A converted minimum is a floor, so rounding must never weaken it.
            BigDecimal minimumUnit=d(product,"minimum_price").multiply(factor).divide(rate,ledger.precision(p.currency()),RoundingMode.CEILING);
            if(basePrice.compareTo(d(product,"minimum_price"))>=0)standard=standard.max(minimumUnit);
            BigDecimal price=sale&&input.unitPrice()==null?standard:number(input.unitPrice(),ledger.precision(p.currency()),false);
            BigDecimal discount=input.discount()==null?BigDecimal.ZERO:number(input.discount(),ledger.precision(p.currency()),false);
            BigDecimal gross=round(p.currency(),price.multiply(input.quantity())).subtract(discount);
            if(gross.signum()<0)throw invalid("Discount exceeds line amount.");
            if(sale){
                if((price.compareTo(standard)!=0||discount.signum()>0)&&(!Set.of("OWNER","MANAGER").contains(role)||input.reason()==null||input.reason().isBlank()))throw new ApiException(403,"PRICE_OVERRIDE_FORBIDDEN","Price overrides need manager permission and a reason.");
                if(gross.compareTo(d(product,"minimum_price").multiply(qty).divide(rate,ledger.precision(p.currency()),RoundingMode.CEILING))<0)throw invalid("Price is below the authorized minimum.");
            }
            BigDecimal taxRate=d(product,"tax_rate").movePointLeft(2),net,tax;
            if(Boolean.TRUE.equals(product.get("tax_inclusive"))){net=round(p.currency(),gross.divide(BigDecimal.ONE.add(taxRate),12,RoundingMode.HALF_UP));tax=gross.subtract(net);}
            else {net=gross;tax=round(p.currency(),net.multiply(taxRate));}
            out.add(new Priced(input,product,unit,qty,standard,price,discount,net,tax,ledger.rounded(business,net.multiply(rate)),ledger.rounded(business,tax.multiply(rate))));
        }
        return out;
    }
    public Map<String,Object> invoice(UUID actor,UUID business,Invoice p){
        if(p==null||p.kind()==null||!Set.of("SALE","PURCHASE").contains(p.kind()))throw invalid("Invalid invoice kind.");
        boolean sale=p.kind().equals("SALE");String r=write(actor,business,sale?new String[]{"OWNER","MANAGER","CASHIER"}:new String[]{"OWNER","MANAGER","ACCOUNTANT"});
        ledger.requireActive(business);if(sale)access.require(actor,business,"RETAIL",false);
        UUID branch=access.location(actor,business,p.locationId(),"INVENTORY");
        var cmd=ledger.command(actor,business,p.requestId(),p.kind(),p);if(cmd.replayed())return result(cmd);
        text(p.reference(),200,false);
        var quote=currencies.quote(actor,business,p.currency(),BigDecimal.ZERO,OffsetDateTime.now());
        var priced=price(actor,business,branch,p,r,quote.rateToBase());
        Map<String,Object> party=null;
        if(p.partyId()!=null)party=one("SELECT * FROM commerce_parties WHERE business_id=? AND id=? AND kind=? AND active=true",business,p.partyId(),sale?"CUSTOMER":"SUPPLIER");
        if(!sale&&party==null)throw invalid("A supplier is required.");
        BigDecimal net=BigDecimal.ZERO,tax=BigDecimal.ZERO,baseTotal=BigDecimal.ZERO;
        for(var line:priced){net=net.add(line.net());tax=tax.add(line.tax());baseTotal=baseTotal.add(line.baseNet()).add(line.baseTax());}
        if(net.add(tax).signum()<=0||baseTotal.signum()<=0)throw invalid("Invoice total must be positive.");
        var names=one("SELECT b.name AS business,br.name AS branch,l.name AS location,u.full_name AS cashier FROM commerce_businesses b JOIN commerce_branches br ON br.business_id=b.id JOIN commerce_stock_locations l ON l.business_id=b.id AND l.branch_id=br.id CROSS JOIN users u WHERE b.id=? AND l.id=? AND u.id=?",business,p.locationId(),actor);
        db.update("""
          INSERT INTO commerce_documents(id,business_id,branch_id,location_id,kind,party_id,party_name,reference,due_date,currency,exchange_rate,rate_id,subtotal,tax,total,base_total,created_by,business_name,branch_name,location_name,cashier_name)
          VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
          """,cmd.id(),business,branch,p.locationId(),p.kind(),p.partyId(),party==null?null:party.get("name"),p.reference(),p.dueDate(),p.currency(),quote.rateToBase(),quote.rateId(),net,tax,net.add(tax),baseTotal,actor,names.get("business"),names.get("branch"),names.get("location"),names.get("cashier"));
        var entries=new ArrayList<Entry>();int position=0;
        entries.add(sale?debit(branch,"RECEIVABLE",baseTotal):credit(branch,"PAYABLE",baseTotal));
        for(var line:priced){
            boolean recoverable=Boolean.TRUE.equals(line.product().get("purchase_tax_recoverable"));
            BigDecimal value=sale?move(actor,business,cmd.id(),p.locationId(),line.input().productId(),line.quantity().negate(),"SALE",null):line.baseNet().add(recoverable?BigDecimal.ZERO:line.baseTax());
            db.update("""
              INSERT INTO commerce_document_lines(id,business_id,document_id,position,product_id,sku,description,unit,quantity,base_quantity,standard_price,unit_price,discount,discount_reason,tax_rate,tax_recoverable,net,tax,base_net,base_tax,inventory_value,category_name)
              VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
              """,UUID.randomUUID(),business,cmd.id(),position++,line.input().productId(),line.product().get("sku"),line.product().get("name"),line.unit(),line.input().quantity(),line.quantity(),line.standard(),line.price(),line.discount(),line.input().reason(),line.product().get("tax_rate"),recoverable,line.net(),line.tax(),line.baseNet(),line.baseTax(),value,line.product().get("category_name"));
            if(sale){entries.add(credit(branch,"SALES",line.baseNet()));entries.add(credit(branch,"OUTPUT_TAX",line.baseTax()));entries.add(debit(branch,"COGS",value));entries.add(credit(branch,"INVENTORY",value));}
            else {entries.add(debit(branch,"GOODS_CLEARING",value));if(recoverable)entries.add(debit(branch,"INPUT_TAX",line.baseTax()));}
        }
        ledger.journal(actor,business,p.kind(),cmd.id(),p.kind()+" invoice",entries);
        var doc=document(actor,business,cmd.id(),true);
        if(p.payments()!=null){if(p.payments().size()>10)throw invalid("Too many split payments.");for(var payment:p.payments())pay(actor,business,cmd.id(),doc,payment,false);}
        if(sale&&outstanding(doc).signum()>0){
            if(party==null)throw invalid("Unpaid sales require a customer with an approved credit limit.");
            BigDecimal exposure=BigDecimal.ZERO;
            for(var customerDoc:db.queryForList("SELECT * FROM commerce_documents WHERE business_id=? AND party_id=? AND kind='SALE'",business,p.partyId()))exposure=exposure.add(carrying(customerDoc));
            if(exposure.compareTo(d(party,"credit_limit_base"))>0)throw new ApiException(409,"CREDIT_LIMIT_EXCEEDED","Customer credit limit exceeded.");
        }
        return result(cmd);
    }
    @Transactional(readOnly=true)
    public Map<String,Object> preview(UUID actor,UUID business,Invoice p){
        if(p==null||p.kind()==null||!Set.of("SALE","PURCHASE").contains(p.kind()))throw invalid("Invalid invoice kind.");
        String r=role(actor,business,p.kind().equals("SALE")?new String[]{"OWNER","MANAGER","CASHIER"}:new String[]{"OWNER","MANAGER","ACCOUNTANT"});
        UUID branch=access.location(actor,business,p.locationId(),"INVENTORY");
        var quote=currencies.quote(actor,business,p.currency(),BigDecimal.ZERO,OffsetDateTime.now());
        var lines=price(actor,business,branch,p,r,quote.rateToBase());
        var output=new ArrayList<Map<String,Object>>();BigDecimal total=BigDecimal.ZERO;
        for(var line:lines){total=total.add(line.net()).add(line.tax());output.add(Map.of("name",line.product().get("name"),"quantity",line.input().quantity(),"unitPrice",line.price(),"discount",line.discount(),"net",line.net(),"tax",line.tax(),"total",line.net().add(line.tax())));}
        return Map.of("lines",output,"currency",p.currency(),"total",total,"exchangeRate",quote.rateToBase());
    }
    private BigDecimal move(UUID actor,UUID business,UUID command,UUID location,UUID product,BigDecimal delta,String kind,BigDecimal incoming){
        BigDecimal value=ledger.value(business,location,product,delta,incoming);
        if(db.update("UPDATE commerce_stock_balances SET quantity=quantity+? WHERE business_id=? AND location_id=? AND product_id=? AND quantity+?>=0",delta,business,location,product,delta)!=1)throw new ApiException(409,"INSUFFICIENT_STOCK","Insufficient stock at the selected location.");
        db.update("INSERT INTO commerce_stock_movements(id,business_id,location_id,product_id,movement_type,quantity_delta,request_id,created_by,financial_command_id) VALUES(?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),business,location,product,kind,delta,UUID.randomUUID(),actor,command);return value;
    }
    public Map<String,Object> receive(UUID actor,UUID business,Receive p){
        write(actor,business,"OWNER","MANAGER","STOREKEEPER");ledger.requireActive(business);
        var doc=document(actor,business,p.documentId(),true);if(!doc.get("kind").equals("PURCHASE"))throw invalid("Only purchase invoices can receive goods.");
        var cmd=ledger.command(actor,business,p.requestId(),"RECEIVE",p);if(cmd.replayed())return result(cmd);
        var line=one("SELECT * FROM commerce_document_lines WHERE business_id=? AND document_id=? AND id=?",business,p.documentId(),p.lineId());
        number(p.quantity(),6,true);
        var received=db.queryForMap("SELECT COALESCE(SUM(quantity),0) AS quantity,COALESCE(SUM(inventory_value),0) AS value FROM commerce_receipt_lines WHERE business_id=? AND line_id=?",business,p.lineId());
        BigDecimal remaining=d(line,"quantity").subtract(d(received,"quantity"));if(p.quantity().compareTo(remaining)>0)throw invalid("Received quantity exceeds the unreceived quantity.");
        BigDecimal qty=number(p.quantity().multiply(d(line,"base_quantity")).divide(d(line,"quantity"),6,RoundingMode.UNNECESSARY),6,true);
        BigDecimal value=p.quantity().compareTo(remaining)==0?d(line,"inventory_value").subtract(d(received,"value")):ledger.rounded(business,d(line,"inventory_value").multiply(p.quantity()).divide(d(line,"quantity"),12,RoundingMode.HALF_UP)).min(d(line,"inventory_value").subtract(d(received,"value")));
        move(actor,business,cmd.id(),id(doc,"location_id"),id(line,"product_id"),qty,"PURCHASE_RECEIPT",value);
        db.update("INSERT INTO commerce_receipt_lines(id,business_id,command_id,line_id,quantity,inventory_value) VALUES(?,?,?,?,?,?)",UUID.randomUUID(),business,cmd.id(),p.lineId(),p.quantity(),value);
        ledger.journal(actor,business,"RECEIVE",cmd.id(),"Verified goods received",List.of(debit(id(doc,"branch_id"),"INVENTORY",value),credit(id(doc,"branch_id"),"GOODS_CLEARING",value)));return result(cmd);
    }
    private BigDecimal outstanding(Map<String,Object> doc){
        BigDecimal paid=db.queryForObject("SELECT COALESCE(SUM(CASE WHEN refund THEN -document_amount ELSE document_amount END),0) FROM commerce_payments WHERE business_id=? AND document_id=?",BigDecimal.class,doc.get("business_id"),doc.get("id"));
        BigDecimal returned=db.queryForObject("SELECT COALESCE(SUM(r.document_amount),0) FROM commerce_returns r JOIN commerce_document_lines l ON l.business_id=r.business_id AND l.id=r.line_id WHERE l.business_id=? AND l.document_id=?",BigDecimal.class,doc.get("business_id"),doc.get("id"));
        return d(doc,"total").subtract(paid).subtract(returned);
    }
    private BigDecimal carrying(Map<String,Object> doc){
        BigDecimal paid=db.queryForObject("SELECT COALESCE(SUM(CASE WHEN refund THEN -carrying_amount ELSE carrying_amount END),0) FROM commerce_payments WHERE business_id=? AND document_id=?",BigDecimal.class,doc.get("business_id"),doc.get("id"));
        BigDecimal returned=db.queryForObject("SELECT COALESCE(SUM(r.base_net+r.base_tax),0) FROM commerce_returns r JOIN commerce_document_lines l ON l.business_id=r.business_id AND l.id=r.line_id WHERE l.business_id=? AND l.document_id=?",BigDecimal.class,doc.get("business_id"),doc.get("id"));
        return d(doc,"base_total").subtract(paid).subtract(returned);
    }
    private void statuses(Map<String,Object> doc){
        BigDecimal due=outstanding(doc);
        BigDecimal paid=db.queryForObject("SELECT COALESCE(SUM(CASE WHEN refund THEN -document_amount ELSE document_amount END),0) FROM commerce_payments WHERE business_id=? AND document_id=?",BigDecimal.class,doc.get("business_id"),doc.get("id"));
        BigDecimal returned=db.queryForObject("SELECT COALESCE(SUM(r.document_amount),0) FROM commerce_returns r JOIN commerce_document_lines l ON l.business_id=r.business_id AND l.id=r.line_id WHERE l.business_id=? AND l.document_id=?",BigDecimal.class,doc.get("business_id"),doc.get("id"));
        doc.put("outstanding",due);
        doc.put("payment_status",due.signum()<0?"REFUND_DUE":due.signum()==0?"SETTLED":paid.signum()>0?"PARTIALLY_PAID":"UNPAID");
        doc.put("document_status",returned.compareTo(d(doc,"total"))==0?"RETURNED":returned.signum()>0?"PARTIALLY_RETURNED":"POSTED");
    }
    public Map<String,Object> settle(UUID actor,UUID business,Settlement p){
        write(actor,business,p.refund()?new String[]{"OWNER","MANAGER","ACCOUNTANT"}:new String[]{"OWNER","MANAGER","ACCOUNTANT","CASHIER"});ledger.requireActive(business);
        var doc=document(actor,business,p.documentId(),true);var cmd=ledger.command(actor,business,p.requestId(),"SETTLEMENT",p);if(cmd.replayed())return result(cmd);
        pay(actor,business,cmd.id(),doc,p.payment(),p.refund());return result(cmd);
    }
    private void pay(UUID actor,UUID business,UUID command,Map<String,Object> doc,Tender p,boolean refund){
        if(p==null)throw invalid("Payment details are required.");text(p.reference(),200,false);
        var account=one("SELECT * FROM commerce_payment_accounts WHERE business_id=? AND id=? AND active=true",business,p.accountId());
        String currency=(String)account.get("currency");number(p.amount(),ledger.precision(currency),true);
        var now=OffsetDateTime.now();var quote=currencies.quote(actor,business,currency,p.amount(),now);
        var docQuote=currencies.quote(actor,business,(String)doc.get("currency"),BigDecimal.ZERO,now);
        BigDecimal allocation=round((String)doc.get("currency"),p.amount().multiply(quote.rateToBase()).divide(docQuote.rateToBase(),12,RoundingMode.HALF_UP));
        BigDecimal due=outstanding(doc).multiply(refund?BigDecimal.ONE.negate():BigDecimal.ONE);
        if(allocation.signum()<=0||allocation.compareTo(due)>0||quote.baseAmount().signum()<=0)throw invalid("Payment exceeds the outstanding amount or has insufficient value.");
        boolean sale=doc.get("kind").equals("SALE");
        if(account.get("method").equals("CASH")){
            var shift=one("SELECT * FROM commerce_shifts WHERE business_id=? AND id=? AND cashier_id=? AND branch_id=? AND payment_account_id=? AND closed_at IS NULL",business,p.shiftId(),actor,doc.get("branch_id"),p.accountId());
        }else if(p.shiftId()!=null)one("SELECT id FROM commerce_shifts WHERE business_id=? AND id=? AND cashier_id=? AND branch_id=? AND closed_at IS NULL",business,p.shiftId(),actor,doc.get("branch_id"));
        BigDecimal historical=allocation.compareTo(due)==0?carrying(doc).abs():ledger.rounded(business,d(doc,"base_total").multiply(allocation).divide(d(doc,"total"),12,RoundingMode.HALF_UP));
        historical=historical.min(carrying(doc).abs());
        BigDecimal actual=quote.baseAmount(),difference=actual.subtract(historical);UUID payment=UUID.randomUUID(),branch=id(doc,"branch_id");
        db.update("""
          INSERT INTO commerce_payments(id,business_id,command_id,document_id,payment_account_id,shift_id,reference,refund,settlement_document_rate,currency,amount,exchange_rate,rate_id,document_amount,base_amount,carrying_amount,fx_difference,created_by)
          VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
          """,payment,business,command,doc.get("id"),p.accountId(),p.shiftId(),p.reference(),refund,docQuote.rateToBase(),currency,p.amount(),quote.rateToBase(),quote.rateId(),allocation,actual,historical,difference,actor);
        boolean incoming=sale!=refund;var entries=new ArrayList<Entry>();
        entries.add(new Entry(branch,"CASH_BANK",incoming?actual:BigDecimal.ZERO,incoming?BigDecimal.ZERO:actual,p.accountId(),null));
        entries.add(incoming?credit(branch,sale?"RECEIVABLE":"PAYABLE",historical):debit(branch,sale?"RECEIVABLE":"PAYABLE",historical));
        if(difference.signum()!=0){boolean gain=(difference.signum()>0)==incoming;entries.add(gain?credit(branch,"FX_GAIN",difference.abs()):debit(branch,"FX_LOSS",difference.abs()));}
        ledger.journal(actor,business,"PAYMENT",payment,refund?"Refund settlement":"Invoice settlement",entries);
    }
    private BigDecimal portion(UUID business,Map<String,Object> line,String column,BigDecimal quantity,BigDecimal previous,BigDecimal already){
        return quantity.add(previous).compareTo(d(line,"quantity"))==0?d(line,column).subtract(already):ledger.rounded(business,d(line,column).multiply(quantity).divide(d(line,"quantity"),12,RoundingMode.HALF_UP)).min(d(line,column).subtract(already));
    }
    public Map<String,Object> returnGoods(UUID actor,UUID business,Return p){
        write(actor,business,"OWNER","MANAGER");ledger.requireActive(business);
        var doc=document(actor,business,p.documentId(),true);boolean sale=doc.get("kind").equals("SALE");
        var cmd=ledger.command(actor,business,p.requestId(),"RETURN",p);if(cmd.replayed())return result(cmd);
        number(p.quantity(),6,true);text(p.reason(),500,true);
        var line=one("SELECT * FROM commerce_document_lines WHERE business_id=? AND document_id=? AND id=?",business,p.documentId(),p.lineId());
        var prior=db.queryForMap("SELECT COALESCE(SUM(quantity),0) AS quantity,COALESCE(SUM(document_amount),0) AS amount,COALESCE(SUM(base_net),0) AS net,COALESCE(SUM(base_tax),0) AS tax,COALESCE(SUM(CASE WHEN saleable THEN inventory_value ELSE 0 END),0) AS value FROM commerce_returns WHERE business_id=? AND line_id=?",business,p.lineId());
        BigDecimal eligible=sale?d(line,"quantity"):db.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM commerce_receipt_lines WHERE business_id=? AND line_id=?",BigDecimal.class,business,p.lineId());
        if(p.quantity().add(d(prior,"quantity")).compareTo(eligible)>0)throw invalid("Return exceeds eligible quantity.");
        BigDecimal fraction=p.quantity().divide(d(line,"quantity"),12,RoundingMode.HALF_UP);
        BigDecimal qty=number(p.quantity().multiply(d(line,"base_quantity")).divide(d(line,"quantity"),6,RoundingMode.UNNECESSARY),6,true);
        BigDecimal net=portion(business,line,"base_net",p.quantity(),d(prior,"quantity"),d(prior,"net")),tax=portion(business,line,"base_tax",p.quantity(),d(prior,"quantity"),d(prior,"tax"));
        BigDecimal amount=p.quantity().add(d(prior,"quantity")).compareTo(d(line,"quantity"))==0?d(line,"net").add(d(line,"tax")).subtract(d(prior,"amount")):round((String)doc.get("currency"),d(line,"net").add(d(line,"tax")).multiply(fraction)).min(d(line,"net").add(d(line,"tax")).subtract(d(prior,"amount")));
        UUID branch=id(doc,"branch_id");BigDecimal value=BigDecimal.ZERO;var entries=new ArrayList<Entry>();
        if(sale){
            entries.add(debit(branch,"SALES",net));entries.add(debit(branch,"OUTPUT_TAX",tax));entries.add(credit(branch,"RECEIVABLE",net.add(tax)));
            if(p.saleable()){
                // Allocate original COGS by cumulative saleable quantity; damaged returns never restore inventory.
                BigDecimal saleableQty=db.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM commerce_returns WHERE business_id=? AND line_id=? AND saleable",BigDecimal.class,business,p.lineId());
                value=portion(business,line,"inventory_value",p.quantity(),saleableQty,d(prior,"value"));
                move(actor,business,cmd.id(),id(doc,"location_id"),id(line,"product_id"),qty,"SALE_RETURN",value);
                entries.add(debit(branch,"INVENTORY",value));entries.add(credit(branch,"COGS",value));
            }
        }else{
            value=move(actor,business,cmd.id(),id(doc,"location_id"),id(line,"product_id"),qty.negate(),"SUPPLIER_RETURN",null);
            boolean recoverable=Boolean.TRUE.equals(line.get("tax_recoverable"));BigDecimal historical=net.add(recoverable?BigDecimal.ZERO:tax);
            entries.add(debit(branch,"PAYABLE",net.add(tax)));if(recoverable)entries.add(credit(branch,"INPUT_TAX",tax));entries.add(credit(branch,"INVENTORY",value));
            BigDecimal variance=value.subtract(historical);entries.add(variance.signum()>0?debit(branch,"PURCHASE_VARIANCE",variance):credit(branch,"PURCHASE_VARIANCE",variance.abs()));
        }
        db.update("INSERT INTO commerce_returns(id,business_id,command_id,line_id,quantity,saleable,document_amount,base_net,base_tax,inventory_value,reason) VALUES(?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),business,cmd.id(),p.lineId(),p.quantity(),p.saleable(),amount,net,tax,value,p.reason());
        ledger.journal(actor,business,"RETURN",cmd.id(),p.reason(),entries);return result(cmd);
    }
    public Map<String,Object> detail(UUID actor,UUID business,UUID document){
        role(actor,business,"OWNER","MANAGER","ACCOUNTANT","CASHIER","STOREKEEPER","VIEWER");var doc=document(actor,business,document,false);
        var lines=db.queryForList("""
          SELECT l.*,l.net+l.tax AS line_total,COALESCE((SELECT SUM(r.quantity) FROM commerce_receipt_lines r WHERE r.business_id=l.business_id AND r.line_id=l.id),0) AS received,
          COALESCE((SELECT SUM(r.quantity) FROM commerce_returns r WHERE r.business_id=l.business_id AND r.line_id=l.id),0) AS returned
          FROM commerce_document_lines l WHERE business_id=? AND document_id=? ORDER BY position
          """,business,document);
        boolean financial=access.financial(actor,business);
        if(!financial)lines.forEach(line->{line.remove("inventory_value");line.remove("base_net");line.remove("base_tax");});
        var payments=db.queryForList("SELECT p.*,a.name AS account_name,a.method FROM commerce_payments p JOIN commerce_payment_accounts a ON a.business_id=p.business_id AND a.id=p.payment_account_id WHERE p.business_id=? AND p.document_id=? ORDER BY p.created_at",business,document);
        if(!financial)payments.forEach(p->{p.remove("carrying_amount");p.remove("fx_difference");});
        doc.put("lines",lines);doc.put("payments",payments);statuses(doc);
        doc.put("returns",db.queryForList("SELECT r.* FROM commerce_returns r JOIN commerce_document_lines l ON l.business_id=r.business_id AND l.id=r.line_id WHERE l.business_id=? AND l.document_id=? ORDER BY r.created_at",business,document));
        if(!financial)((List<Map<String,Object>>)doc.get("returns")).forEach(row->row.remove("inventory_value"));
        if(access.role(actor,business,"ACCOUNTING").equals("STOREKEEPER")){
            lines.forEach(line->{for(String field:List.of("standard_price","unit_price","discount","net","tax","tax_rate","line_total"))line.remove(field);});
            for(String field:List.of("payments","returns","outstanding","subtotal","tax","total","base_total","exchange_rate","payment_status"))doc.remove(field);
        }
        return doc;
    }
    public List<Map<String,Object>> history(UUID actor,UUID business,UUID branch){
        String r=role(actor,business,"OWNER","MANAGER","ACCOUNTANT","CASHIER","STOREKEEPER","VIEWER");access.branch(actor,business,branch,"ACCOUNTING",false);
        var rows=db.queryForList("SELECT * FROM commerce_documents WHERE business_id=? AND branch_id=? AND (?<>'CASHIER' OR (created_by=? AND kind='SALE')) AND (?<>'VIEWER' OR kind='SALE') ORDER BY created_at DESC LIMIT 200",business,branch,r,actor,r);
        rows.forEach(row->{if(!r.equals("STOREKEEPER"))statuses(row);else for(String field:List.of("subtotal","tax","total","base_total","exchange_rate"))row.remove(field);});return rows;
    }
}
