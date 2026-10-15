package com.hotelmanagement.hms.commerce.accounting;

import com.hotelmanagement.hms.commerce.catalog.CommerceCatalogAccess;
import com.hotelmanagement.hms.commerce.foundation.CommerceConfigurationService;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.hotelmanagement.hms.commerce.accounting.CommerceLedgerService.*;

@Service
@Transactional
public class CommerceFinanceService {
    private final JdbcTemplate db;private final CommerceCatalogAccess access;private final CommerceLedgerService ledger;private final CommerceConfigurationService currencies;
    public CommerceFinanceService(JdbcTemplate db,CommerceCatalogAccess access,CommerceLedgerService ledger,CommerceConfigurationService currencies){this.db=db;this.access=access;this.ledger=ledger;this.currencies=currencies;}
    public record Shift(UUID requestId,UUID branchId,UUID accountId,BigDecimal openingCash) {}
    public record Close(UUID requestId,BigDecimal countedCash) {}
    public record CashMove(UUID requestId,BigDecimal amount,String reason) {}
    public record Expense(UUID requestId,UUID branchId,UUID accountId,BigDecimal amount,String reason) {}
    private String authorize(UUID actor,UUID business,String... roles){return access.permittedRole(actor,business,"ACCOUNTING",roles);}
    private void write(UUID actor,UUID business,String... roles){authorize(actor,business,roles);ledger.lock(business);authorize(actor,business,roles);ledger.requireActive(business);}
    private BigDecimal valid(BigDecimal amount,String currency){if(amount==null||amount.signum()<0||amount.stripTrailingZeros().scale()>ledger.precision(currency)||amount.precision()-amount.scale()>13)throw invalid("Invalid monetary amount.");return amount;}
    private Map<String,Object> one(String sql,Object... args){var rows=db.queryForList(sql,args);if(rows.isEmpty())throw ApiException.notFound();return rows.getFirst();}
    private Map<String,Object> result(Command c){return Map.of("id",c.id(),"replayed",c.replayed());}
    public Map<String,Object> state(UUID actor,UUID business){
        authorize(actor,business,"OWNER","MANAGER","ACCOUNTANT","CASHIER","STOREKEEPER","VIEWER");var out=new LinkedHashMap<String,Object>();out.put("active",ledger.active(business));out.put("baseCurrency",ledger.base(business));
        if(access.role(actor,business,"ACCOUNTING").equals("OWNER"))out.put("openingValuation",db.queryForObject("SELECT COALESCE(SUM(ROUND(s.quantity*p.purchase_cost,?)),0) FROM commerce_stock_balances s JOIN commerce_products p ON p.business_id=s.business_id AND p.id=s.product_id WHERE s.business_id=?",BigDecimal.class,ledger.precision(ledger.base(business)),business));return out;
    }
    public Map<String,Object> open(UUID actor,UUID business,Shift p){
        write(actor,business,"OWNER","MANAGER","CASHIER");access.branch(actor,business,p.branchId(),"ACCOUNTING");
        var cmd=ledger.command(actor,business,p.requestId(),"OPEN_SHIFT",p);if(cmd.replayed())return result(cmd);
        var account=one("SELECT currency FROM commerce_payment_accounts WHERE business_id=? AND id=? AND method='CASH' AND active",business,p.accountId());valid(p.openingCash(),(String)account.get("currency"));
        if(!db.queryForList("SELECT id FROM commerce_shifts WHERE business_id=? AND cashier_id=? AND closed_at IS NULL",business,actor).isEmpty())throw invalid("Close your existing shift first.");
        db.update("INSERT INTO commerce_shifts(id,business_id,branch_id,cashier_id,payment_account_id,opening_cash) VALUES(?,?,?,?,?,?)",cmd.id(),business,p.branchId(),actor,p.accountId(),p.openingCash());return result(cmd);
    }
    private Map<String,Object> shift(UUID actor,UUID business,UUID id){
        var row=one("SELECT s.*,a.currency FROM commerce_shifts s JOIN commerce_payment_accounts a ON a.business_id=s.business_id AND a.id=s.payment_account_id WHERE s.business_id=? AND s.id=?",business,id);
        access.branch(actor,business,(UUID)row.get("branch_id"),"ACCOUNTING",false);
        if(!actor.equals(row.get("cashier_id"))&&!access.financial(actor,business))throw ApiException.notFound();return row;
    }
    private BigDecimal expected(Map<String,Object> shift){
        BigDecimal payments=db.queryForObject("""
          SELECT COALESCE(SUM(CASE WHEN (d.kind='SALE')<>p.refund THEN p.amount ELSE -p.amount END),0)
          FROM commerce_payments p JOIN commerce_documents d ON d.business_id=p.business_id AND d.id=p.document_id
          WHERE p.business_id=? AND p.shift_id=? AND p.payment_account_id=?
          """,BigDecimal.class,shift.get("business_id"),shift.get("id"),shift.get("payment_account_id"));
        BigDecimal moves=db.queryForObject("SELECT COALESCE(SUM(amount),0) FROM commerce_cash_moves WHERE business_id=? AND shift_id=?",BigDecimal.class,shift.get("business_id"),shift.get("id"));
        return decimal(shift.get("opening_cash")).add(payments).add(moves);
    }
    public Map<String,Object> close(UUID actor,UUID business,UUID id,Close p){
        write(actor,business,"OWNER","MANAGER","CASHIER");var shift=shift(actor,business,id);
        if(!actor.equals(shift.get("cashier_id")))throw invalid("Only the shift cashier can submit the count.");
        var cmd=ledger.command(actor,business,p.requestId(),"CLOSE_SHIFT",Map.of("shift",id,"count",p));if(cmd.replayed())return result(cmd);
        if(shift.get("closed_at")!=null)throw invalid("Shift is already closed.");valid(p.countedCash(),(String)shift.get("currency"));
        var expected=expected(shift);db.update("UPDATE commerce_shifts SET closed_at=CURRENT_TIMESTAMP,counted_cash=?,expected_cash=?,variance=? WHERE business_id=? AND id=?",p.countedCash(),expected,p.countedCash().subtract(expected),business,id);return result(cmd);
    }
    public Map<String,Object> approve(UUID actor,UUID business,UUID id,UUID request){
        write(actor,business,"OWNER","MANAGER");var shift=shift(actor,business,id);
        var cmd=ledger.command(actor,business,request,"APPROVE_SHIFT",Map.of("shift",id));if(cmd.replayed())return result(cmd);
        if(shift.get("closed_at")==null||shift.get("approved_at")!=null)throw invalid("An unapproved closed shift is required.");
        if(actor.equals(shift.get("cashier_id")))throw invalid("A different manager must approve the cash count.");
        db.update("UPDATE commerce_shifts SET approved_by=?,approved_at=CURRENT_TIMESTAMP WHERE business_id=? AND id=?",actor,business,id);return result(cmd);
    }
    public Map<String,Object> cashMove(UUID actor,UUID business,UUID id,CashMove p){
        write(actor,business,"OWNER","MANAGER");var shift=shift(actor,business,id);
        var cmd=ledger.command(actor,business,p.requestId(),"CASH_MOVE",Map.of("shift",id,"movement",p));if(cmd.replayed())return result(cmd);
        if(shift.get("closed_at")!=null||p.amount()==null||p.amount().signum()==0||p.reason()==null||p.reason().isBlank()||p.reason().length()>500)throw invalid("An open shift, amount and reason are required.");valid(p.amount().abs(),(String)shift.get("currency"));
        if(expected(shift).add(p.amount()).signum()<0)throw invalid("Withdrawal exceeds the expected drawer balance.");
        db.update("INSERT INTO commerce_cash_moves(id,business_id,shift_id,amount,reason,created_by) VALUES(?,?,?,?,?,?)",cmd.id(),business,id,p.amount(),p.reason(),actor);
        // Drawer custody transfer within the same cash account; not revenue or an expense.
        return result(cmd);
    }
    public List<Map<String,Object>> shifts(UUID actor,UUID business,UUID branch){
        String r=authorize(actor,business,"OWNER","MANAGER","ACCOUNTANT","CASHIER","VIEWER");access.branch(actor,business,branch,"ACCOUNTING",false);
        var rows=db.queryForList("SELECT s.*,a.currency,a.name AS account_name FROM commerce_shifts s JOIN commerce_payment_accounts a ON a.business_id=s.business_id AND a.id=s.payment_account_id WHERE s.business_id=? AND s.branch_id=? AND (?<>'CASHIER' OR s.cashier_id=?) ORDER BY s.opened_at DESC LIMIT 100",business,branch,r,actor);
        rows.forEach(row->row.put("expected",expected(row)));return rows;
    }
    public Map<String,Object> expense(UUID actor,UUID business,Expense p){
        write(actor,business,"OWNER","MANAGER","ACCOUNTANT");access.branch(actor,business,p.branchId(),"ACCOUNTING");
        var cmd=ledger.command(actor,business,p.requestId(),"EXPENSE",p);if(cmd.replayed())return result(cmd);
        if(p.reason()==null||p.reason().isBlank()||p.reason().length()>500)throw invalid("Expense description is required.");
        var account=one("SELECT * FROM commerce_payment_accounts WHERE business_id=? AND id=? AND active AND method<>'CASH'",business,p.accountId());
        valid(p.amount(),(String)account.get("currency"));if(p.amount().signum()<=0)throw invalid("Expense amount must be positive.");
        var quote=currencies.quote(actor,business,(String)account.get("currency"),p.amount(),OffsetDateTime.now());
        db.update("INSERT INTO commerce_expenses(id,business_id,branch_id,payment_account_id,currency,amount,exchange_rate,rate_id,base_amount,reason) VALUES(?,?,?,?,?,?,?,?,?,?)",cmd.id(),business,p.branchId(),p.accountId(),quote.currency(),quote.amount(),quote.rateToBase(),quote.rateId(),quote.baseAmount(),p.reason());
        ledger.journal(actor,business,"EXPENSE",cmd.id(),p.reason(),List.of(debit(p.branchId(),"EXPENSE",quote.baseAmount()),new Entry(p.branchId(),"CASH_BANK",BigDecimal.ZERO,quote.baseAmount(),p.accountId(),null)));return result(cmd);
    }
    public Map<String,Object> reports(UUID actor,UUID business,UUID branch,LocalDate from,LocalDate to){
        String r=authorize(actor,business,"OWNER","MANAGER","ACCOUNTANT");
        if(branch!=null)access.branch(actor,business,branch,"ACCOUNTING",false);
        if(from==null||to==null||to.isBefore(from)||from.plusYears(2).isBefore(to))throw invalid("Choose a date range of at most two years.");
        var start=from.atStartOfDay().atOffset(ZoneOffset.UTC);var end=to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        // NULL branch means all explicitly assigned branches (all branches for the owner).
        String scope=" (?::uuid IS NULL OR l.branch_id=?) AND (?='OWNER' OR EXISTS(SELECT 1 FROM commerce_branch_access a WHERE a.business_id=l.business_id AND a.branch_id=l.branch_id AND a.user_id=?)) ";
        var out=new LinkedHashMap<String,Object>();out.put("baseCurrency",ledger.base(business));out.put("from",from);out.put("to",to);
        out.put("accounts",db.queryForList("SELECT l.branch_id,b.name AS branch,l.account,SUM(l.debit) AS debit,SUM(l.credit) AS credit,SUM(l.debit-l.credit) AS balance FROM commerce_journal_lines l JOIN commerce_journals j ON j.business_id=l.business_id AND j.id=l.journal_id JOIN commerce_branches b ON b.business_id=l.business_id AND b.id=l.branch_id WHERE l.business_id=? AND j.created_at>=? AND j.created_at<? AND "+scope+" GROUP BY l.branch_id,b.name,l.account ORDER BY b.name,l.account",business,start,end,branch,branch,r,actor));
        // Cumulative recorded account movements through the report end date. Drawer
        // opening floats and custody moves are not new account funds.
        out.put("paymentAccounts",db.queryForList("""
          WITH movements AS (
            SELECT p.business_id,d.branch_id,p.payment_account_id,p.created_at,
              CASE WHEN (d.kind='SALE')<>p.refund THEN p.amount ELSE -p.amount END AS amount
            FROM commerce_payments p JOIN commerce_documents d ON d.business_id=p.business_id AND d.id=p.document_id
            UNION ALL
            SELECT business_id,branch_id,payment_account_id,created_at,-amount FROM commerce_expenses
          ), totals AS (
            SELECT l.payment_account_id,SUM(l.amount) AS balance FROM movements l
            WHERE l.business_id=? AND l.created_at<? AND
          """+scope+"""
            GROUP BY l.payment_account_id
          )
          SELECT a.id,a.name,a.method,a.currency,a.active,COALESCE(t.balance,0) AS balance
          FROM commerce_payment_accounts a LEFT JOIN totals t ON t.payment_account_id=a.id
          WHERE a.business_id=? ORDER BY a.method,a.currency,a.name
          """,business,end,branch,branch,r,actor,business));
        out.put("inventory",db.queryForList("SELECT l.branch_id,l.name AS location,p.sku,p.name,s.quantity,s.inventory_value,p.low_stock_quantity,(s.quantity<=p.low_stock_quantity) AS low_stock FROM commerce_stock_balances s JOIN commerce_stock_locations l ON l.business_id=s.business_id AND l.id=s.location_id JOIN commerce_products p ON p.business_id=s.business_id AND p.id=s.product_id WHERE l.business_id=? AND "+scope+" ORDER BY l.name,p.name",business,branch,branch,r,actor));
        out.put("sales",db.queryForList("SELECT l.branch_id,l.created_by,l.party_name,l.currency,l.kind,COUNT(*) AS documents,SUM(l.total) AS total,SUM(l.base_total) AS base_total FROM commerce_documents l WHERE l.business_id=? AND l.created_at>=? AND l.created_at<? AND "+scope+" GROUP BY l.branch_id,l.created_by,l.party_name,l.currency,l.kind ORDER BY l.branch_id",business,start,end,branch,branch,r,actor));
        out.put("collections",db.queryForList("SELECT l.branch_id,a.name,a.currency,a.method,p.refund,l.kind,SUM(p.amount) AS amount,SUM(p.base_amount) AS base_amount FROM commerce_payments p JOIN commerce_documents l ON l.business_id=p.business_id AND l.id=p.document_id JOIN commerce_payment_accounts a ON a.business_id=p.business_id AND a.id=p.payment_account_id WHERE l.business_id=? AND p.created_at>=? AND p.created_at<? AND "+scope+" GROUP BY l.branch_id,a.name,a.currency,a.method,p.refund,l.kind",business,start,end,branch,branch,r,actor));
        out.put("balances",db.queryForList("""
          SELECT l.branch_id,l.document_number,l.kind,l.party_name,l.currency,l.due_date,
          l.total-COALESCE((SELECT SUM(CASE WHEN p.refund THEN -p.document_amount ELSE p.document_amount END) FROM commerce_payments p WHERE p.business_id=l.business_id AND p.document_id=l.id),0)
          -COALESCE((SELECT SUM(r.document_amount) FROM commerce_returns r JOIN commerce_document_lines dl ON dl.business_id=r.business_id AND dl.id=r.line_id WHERE dl.business_id=l.business_id AND dl.document_id=l.id),0) AS outstanding
          FROM commerce_documents l WHERE l.business_id=? AND
          """+scope+" ORDER BY l.kind,l.party_name,l.document_number",business,branch,branch,r,actor));
        out.put("products",db.queryForList("""
          SELECT l.branch_id,dl.sku,dl.description,dl.category_name,SUM(dl.quantity) AS quantity,SUM(dl.base_net) AS revenue,SUM(dl.inventory_value) AS cost
          FROM commerce_document_lines dl JOIN commerce_documents l ON l.business_id=dl.business_id AND l.id=dl.document_id
          WHERE l.business_id=? AND l.kind='SALE' AND l.created_at>=? AND l.created_at<? AND
          """+scope+" GROUP BY l.branch_id,dl.sku,dl.description,dl.category_name ORDER BY dl.sku",business,start,end,branch,branch,r,actor));
        out.put("returns",db.queryForList("SELECT l.branch_id,l.document_number,l.kind,l.currency,r.quantity,r.document_amount,r.base_net,r.base_tax,r.reason,r.created_at FROM commerce_returns r JOIN commerce_document_lines dl ON dl.business_id=r.business_id AND dl.id=r.line_id JOIN commerce_documents l ON l.business_id=dl.business_id AND l.id=dl.document_id WHERE l.business_id=? AND r.created_at>=? AND r.created_at<? AND "+scope+" ORDER BY r.created_at",business,start,end,branch,branch,r,actor));
        BigDecimal revenue=BigDecimal.ZERO,cost=BigDecimal.ZERO;
        for(var entry:(List<Map<String,Object>>)out.get("accounts")){
            if(entry.get("account").equals("SALES"))revenue=revenue.subtract(decimal(entry.get("balance")));
            if(entry.get("account").equals("COGS"))cost=cost.add(decimal(entry.get("balance")));
        }
        out.put("summary",List.of(Map.of("revenue",revenue,"cost",cost,"gross_profit",revenue.subtract(cost),"margin_percent",revenue.signum()>0?revenue.subtract(cost).multiply(new BigDecimal("100")).divide(revenue,2,RoundingMode.HALF_UP):BigDecimal.ZERO)));
        return out;
    }
}
