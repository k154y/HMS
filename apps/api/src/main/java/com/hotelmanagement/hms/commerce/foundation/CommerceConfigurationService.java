package com.hotelmanagement.hms.commerce.foundation;

import com.hotelmanagement.hms.commerce.catalog.CommerceCatalogAccess;
import com.hotelmanagement.hms.identity.service.UserAccountService;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.OffsetDateTime;
import java.util.*;

/** Tenant configuration only; no hotel privileges or platform administrator bypass. */
@Service
@Transactional
public class CommerceConfigurationService {
    private final JdbcTemplate db;
    private final CommerceCatalogAccess access;
    private final UserAccountService users;
    public CommerceConfigurationService(JdbcTemplate db,CommerceCatalogAccess access,UserAccountService users) {
        this.db=db;this.access=access;this.users=users;
    }
    private ApiException invalid(String text) {return new ApiException(400,"INVALID_CONFIGURATION",text);}
    private String text(String value,int max) {
        if(value==null||value.isBlank()||value.strip().length()>max) throw invalid("Required text is missing or too long.");
        return value.strip();
    }
    private void lock(UUID business) {
        // Serialize graph/configuration edits within a business, including competing cycle creation.
        db.queryForObject("SELECT id FROM commerce_businesses WHERE id=? FOR UPDATE",UUID.class,business);
    }
    private void audit(UUID actor,UUID business,String action,UUID id,String detail) {
        db.update("INSERT INTO commerce_configuration_audit(id,business_id,actor_id,action,entity_id,detail) VALUES(?,?,?,?,?,jsonb_build_object('change',CAST(? AS text)))",
                UUID.randomUUID(),business,actor,action,id,detail);
    }
    public void category(UUID actor,UUID business,UUID category,String name,UUID parent,boolean active) {
        access.require(actor,business,"CORE",true);lock(business);name=text(name,160);
        access.require(actor,business,"CORE",true);
        var old=db.queryForList("SELECT id,parent_id,name,active FROM commerce_categories WHERE business_id=? AND id=?",business,category);
        if(old.isEmpty()) throw ApiException.notFound();
        if(parent!=null) {
            if(db.queryForList("SELECT id FROM commerce_categories WHERE business_id=? AND id=? AND active=true",business,parent).isEmpty()) throw ApiException.notFound();
            Boolean cycle=db.queryForObject("""
                WITH RECURSIVE ancestors AS (
                  SELECT id,parent_id FROM commerce_categories WHERE business_id=? AND id=?
                  UNION SELECT c.id,c.parent_id FROM commerce_categories c JOIN ancestors a ON c.id=a.parent_id WHERE c.business_id=?
                ) SELECT EXISTS(SELECT 1 FROM ancestors WHERE id=?)
                """,Boolean.class,business,parent,business,category);
            if(Boolean.TRUE.equals(cycle)) throw invalid("A category cannot be its own ancestor.");
        }
        if(!active && Boolean.TRUE.equals(db.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM commerce_categories WHERE business_id=? AND parent_id=? AND active=true)
                OR EXISTS(SELECT 1 FROM commerce_products WHERE business_id=? AND category_id=? AND active=true)
                """,Boolean.class,business,category,business,category)))
            throw new ApiException(409,"CATEGORY_IN_USE","Move or deactivate active children and products first.");
        db.update("UPDATE commerce_categories SET name=?,parent_id=?,active=? WHERE business_id=? AND id=?",name,parent,active,business,category);
        audit(actor,business,"CATEGORY_UPDATED",category,old.getFirst()+" -> name="+name+", parent="+parent+", active="+active);
    }
    public void branch(UUID actor,UUID business,UUID branch,String name,String contact,boolean active) {
        access.owner(actor,business);lock(business);name=text(name,200);
        if(contact!=null&&contact.length()>300) throw invalid("Contact must not exceed 300 characters.");
        var old=db.queryForList("SELECT name,contact,active FROM commerce_branches WHERE business_id=? AND id=?",business,branch);
        if(old.isEmpty()) throw ApiException.notFound();
        if(!active && Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM commerce_stock_locations WHERE business_id=? AND branch_id=? AND active=true)",Boolean.class,business,branch)))
            throw new ApiException(409,"BRANCH_IN_USE","Deactivate this branch's stock locations first.");
        db.update("UPDATE commerce_branches SET name=?,contact=?,active=? WHERE business_id=? AND id=?",name,contact,active,business,branch);
        audit(actor,business,"BRANCH_UPDATED",branch,old.getFirst()+" -> name="+name+", contact="+contact+", active="+active);
    }
    public void location(UUID actor,UUID business,UUID location,String name,boolean active) {
        access.owner(actor,business);lock(business);name=text(name,200);
        var old=db.queryForList("SELECT name,active,branch_id FROM commerce_stock_locations WHERE business_id=? AND id=?",business,location);
        if(old.isEmpty()) throw ApiException.notFound();
        if(active) access.branch(actor,business,(UUID)old.getFirst().get("branch_id"),"CORE");
        if(!active && Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM commerce_stock_balances WHERE business_id=? AND location_id=? AND quantity<>0)",Boolean.class,business,location)))
            throw new ApiException(409,"LOCATION_HAS_STOCK","Move stock before deactivating this location.");
        // Branch is intentionally immutable: relinking would silently move the location's history.
        db.update("UPDATE commerce_stock_locations SET name=?,active=? WHERE business_id=? AND id=?",name,active,business,location);
        audit(actor,business,"LOCATION_UPDATED",location,old.getFirst()+" -> name="+name+", active="+active);
    }
    private Currency currency(String code) {
        try {var c=Currency.getInstance(code);if(c.getDefaultFractionDigits()<0||c.getDefaultFractionDigits()>4) throw invalid("Unsupported currency precision.");return c;}
        catch(IllegalArgumentException|NullPointerException e){throw invalid("Invalid ISO currency.");}
    }
    private String base(UUID business) {return db.queryForObject("SELECT base_currency FROM commerce_businesses WHERE id=?",String.class,business);}
    private boolean baseCurrencyLocked(UUID business) {
        return Boolean.TRUE.equals(db.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM commerce_products WHERE business_id=?)
            OR EXISTS(SELECT 1 FROM commerce_exchange_rates WHERE business_id=?)
            OR EXISTS(SELECT 1 FROM commerce_parties WHERE business_id=?)
            OR EXISTS(SELECT 1 FROM commerce_payment_accounts WHERE business_id=?)
            OR EXISTS(SELECT 1 FROM commerce_accounting_state WHERE business_id=?)
            OR EXISTS(SELECT 1 FROM commerce_financial_commands WHERE business_id=?)
            """,Boolean.class,business,business,business,business,business,business));
    }
    @Transactional(readOnly=true)
    public Map<String,Object> baseCurrencySettings(UUID actor,UUID business) {
        access.owner(actor,business);
        return Map.of("currency",base(business),"locked",baseCurrencyLocked(business));
    }
    public Map<String,Object> setBaseCurrency(UUID actor,UUID business,String code) {
        access.owner(actor,business);lock(business);access.owner(actor,business);
        code=text(code,3).toUpperCase(Locale.ROOT);
        int precision=currency(code).getDefaultFractionDigits();
        String previous=base(business);
        if(!previous.equals(code)) {
            if(baseCurrencyLocked(business)) throw new ApiException(409,"BASE_CURRENCY_LOCKED","Base currency is locked because business records already exist.");
            db.update("UPDATE commerce_businesses SET base_currency=? WHERE id=?",code,business);
            db.update("INSERT INTO commerce_currencies(business_id,currency,decimal_places,active) VALUES(?,?,?,true) ON CONFLICT(business_id,currency) DO UPDATE SET active=true",business,code,precision);
            audit(actor,business,"BASE_CURRENCY_CHANGED",business,previous+" -> "+code);
        }
        return baseCurrencySettings(actor,business);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> currencies(UUID actor,UUID business) {
        access.require(actor,business,"CORE",false);
        var rows=db.queryForList("SELECT currency,decimal_places,active FROM commerce_currencies WHERE business_id=? ORDER BY currency",business);
        String base=base(business);
        // Includes businesses provisioned after V37 without changing their provisioning contract.
        if(rows.stream().noneMatch(row->base.equals(row.get("currency")))) rows.add(new LinkedHashMap<>(Map.of("currency",base,"decimal_places",currency(base).getDefaultFractionDigits(),"active",true)));
        return rows;
    }
    public void configureCurrency(UUID actor,UUID business,String code,boolean active) {
        access.owner(actor,business);lock(business);int precision=currency(code).getDefaultFractionDigits();
        if(code.equals(base(business))&&!active) throw invalid("The base accounting currency cannot be disabled.");
        if(!active&&code.equals(db.queryForObject("SELECT default_transaction_currency FROM commerce_businesses WHERE id=?",String.class,business))) throw invalid("The default transaction currency cannot be disabled.");
        db.update("INSERT INTO commerce_currencies(business_id,currency,decimal_places,active) VALUES(?,?,?,?) ON CONFLICT(business_id,currency) DO UPDATE SET active=EXCLUDED.active",business,code,precision,active);
        audit(actor,business,"CURRENCY_CONFIGURED",null,code+", active="+active);
    }
    public UUID rate(UUID actor,UUID business,String code,BigDecimal rate,OffsetDateTime effective,String source) {
        access.owner(actor,business);lock(business);currency(code);source=text(source,200);
        if(code.equals(base(business))) throw invalid("The base currency always has an exchange rate of one.");
        if(effective==null||rate==null||rate.signum()<=0||rate.scale()>10||rate.precision()-rate.scale()>14) throw invalid("A positive rate and effective date are required.");
        if(db.queryForList("SELECT currency FROM commerce_currencies WHERE business_id=? AND currency=? AND active=true",business,code).isEmpty()) throw invalid("Enable the transaction currency first.");
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO commerce_exchange_rates(id,business_id,currency,effective_at,rate_to_base,source,created_by) VALUES(?,?,?,?,?,?,?)",id,business,code,effective,rate,source,actor);
        audit(actor,business,"EXCHANGE_RATE_ADDED",id,code+", rate="+rate+", effective="+effective+", source="+source);
        return id;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> rates(UUID actor,UUID business) {
        access.require(actor,business,"CORE",false);
        return db.queryForList("SELECT id,currency,effective_at,rate_to_base,source,created_by,created_at FROM commerce_exchange_rates WHERE business_id=? ORDER BY effective_at DESC,id LIMIT 500",business);
    }
    public record Quote(String currency,BigDecimal amount,UUID rateId,BigDecimal rateToBase,String baseCurrency,BigDecimal baseAmount) {}
    /** A quote is a snapshot value, not a posted transaction. Posting must persist every returned field. */
    @Transactional(readOnly=true)
    public Quote quote(UUID actor,UUID business,String code,BigDecimal amount,OffsetDateTime at) {
        access.require(actor,business,"CORE",false);var c=currency(code);String base=base(business);
        if(at==null||amount==null||amount.signum()<0||amount.precision()>19||amount.stripTrailingZeros().scale()>c.getDefaultFractionDigits()) throw invalid("Amount has invalid precision for this currency.");
        if(!code.equals(base)&&db.queryForList("SELECT currency FROM commerce_currencies WHERE business_id=? AND currency=? AND active=true",business,code).isEmpty()) throw invalid("Currency is not enabled.");
        BigDecimal rate=BigDecimal.ONE;UUID id=null;
        if(!code.equals(base)) {
            var rows=db.queryForList("SELECT id,rate_to_base FROM commerce_exchange_rates WHERE business_id=? AND currency=? AND effective_at<=? ORDER BY effective_at DESC LIMIT 1",business,code,at);
            if(rows.isEmpty()) throw new ApiException(409,"EXCHANGE_RATE_MISSING","No exchange rate exists at the transaction date.");
            rate=(BigDecimal)rows.getFirst().get("rate_to_base");id=(UUID)rows.getFirst().get("id");
        }
        return new Quote(code,amount.setScale(c.getDefaultFractionDigits()),id,rate,base,amount.multiply(rate).setScale(currency(base).getDefaultFractionDigits(),RoundingMode.HALF_UP));
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> staff(UUID actor,UUID business) {
        access.owner(actor,business);
        return db.queryForList("""
          SELECT m.user_id,u.email,u.full_name,m.role,m.status,
          COALESCE((SELECT jsonb_agg(a.branch_id ORDER BY a.branch_id) FROM commerce_branch_access a WHERE a.business_id=m.business_id AND a.user_id=m.user_id),'[]'::jsonb)::text AS branch_ids,
          COALESCE((SELECT jsonb_agg(r.role ORDER BY r.role) FROM commerce_membership_roles r WHERE r.business_id=m.business_id AND r.user_id=m.user_id),'[]'::jsonb)::text AS roles
          FROM commerce_memberships m JOIN users u ON u.id=m.user_id WHERE m.business_id=? ORDER BY u.full_name
          """,business);
    }
    private void staffRole(String role) {
        if(!Set.of("MANAGER","CASHIER","STOREKEEPER","ACCOUNTANT","VIEWER").contains(role)) throw invalid("Invalid employee role.");
    }
    private String saveRoles(UUID business,UUID user,List<String> roles){
        if(roles==null||roles.isEmpty()||roles.size()>5||new HashSet<>(roles).size()!=roles.size())
            throw invalid("Select one or more distinct roles.");
        roles.forEach(this::staffRole);
        // The legacy primary role is retained for compatibility; supplementary roles
        // never include OWNER and are always scoped to this business.
        String primary=roles.contains("MANAGER")?"MANAGER":roles.getFirst();
        db.update("DELETE FROM commerce_membership_roles WHERE business_id=? AND user_id=?",business,user);
        for(String role:roles) db.update("INSERT INTO commerce_membership_roles(business_id,user_id,role) VALUES(?,?,?)",business,user,role);
        return primary;
    }
    private void assign(UUID business,UUID user,List<UUID> branches) {
        if(branches==null||branches.isEmpty()) throw invalid("Assign at least one branch.");
        for(UUID branch:new HashSet<>(branches)) {
            if(db.queryForList("SELECT id FROM commerce_branches WHERE business_id=? AND id=? AND active=true",business,branch).isEmpty()) throw ApiException.notFound();
        }
        db.update("DELETE FROM commerce_branch_access WHERE business_id=? AND user_id=?",business,user);
        for(UUID branch:new HashSet<>(branches)) db.update("INSERT INTO commerce_branch_access(business_id,user_id,branch_id) VALUES(?,?,?)",business,user,branch);
    }
    public UUID createStaff(UUID actor,UUID business,String email,String name,String password,List<String> roles,List<UUID> branches) {
        access.owner(actor,business);lock(business);
        // Reuse global identity hashing and password policy, never attach an existing account implicitly.
        UUID user=users.createAccount(email,password,name,null,"en").getId();
        db.update("UPDATE users SET must_change_password=true WHERE id=?",user);
        db.update("INSERT INTO commerce_memberships(business_id,user_id,role) VALUES(?,?,?)",business,user,roles.getFirst());
        String primary=saveRoles(business,user,roles);
        db.update("UPDATE commerce_memberships SET role=? WHERE business_id=? AND user_id=?",primary,business,user);
        assign(business,user,branches);
        audit(actor,business,"EMPLOYEE_CREATED",user,"roles="+roles+", branches="+branches);
        return user;
    }
    public void updateStaff(UUID actor,UUID business,UUID user,List<String> roles,boolean active,List<UUID> branches) {
        access.owner(actor,business);lock(business);
        var rows=db.queryForList("SELECT role,status FROM commerce_memberships WHERE business_id=? AND user_id=?",business,user);
        if(rows.isEmpty()) throw ApiException.notFound();
        if(rows.getFirst().get("role").equals("OWNER")) throw invalid("Owner membership cannot be changed through employee management.");
        // Revoking membership must remain possible after its assigned branches close.
        // Disabled memberships retain their historical branch grants but cannot use them.
        if(active) assign(business,user,branches);
        String primary=saveRoles(business,user,roles);
        db.update("UPDATE commerce_memberships SET role=?,status=? WHERE business_id=? AND user_id=?",primary,active?"ACTIVE":"DISABLED",business,user);
        audit(actor,business,"EMPLOYEE_UPDATED",user,rows.getFirst()+" -> roles="+roles+", active="+active+", branches="+(active?branches:"retained while disabled"));
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> history(UUID actor,UUID business) {
        access.owner(actor,business);
        return db.queryForList("SELECT id,actor_id,action,entity_id,detail::text,created_at FROM commerce_configuration_audit WHERE business_id=? ORDER BY created_at DESC,id LIMIT 500",business);
    }
}
