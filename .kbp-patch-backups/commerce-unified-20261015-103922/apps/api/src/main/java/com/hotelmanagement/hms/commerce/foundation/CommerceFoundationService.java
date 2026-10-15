package com.hotelmanagement.hms.commerce.foundation;

import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Independent commerce ownership; never reinterprets a hotel as a commerce tenant. */
@Service
@Transactional
public class CommerceFoundationService {
    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final boolean selfRegistrationEnabled;
    private final com.hotelmanagement.hms.commerce.catalog.CommerceCatalogAccess scopedAccess;
    public CommerceFoundationService(JdbcTemplate jdbc,
            @Value("${hms.commerce.enabled:false}") boolean enabled,
            @Value("${hms.commerce.self-registration-enabled:false}") boolean selfRegistrationEnabled,
            com.hotelmanagement.hms.commerce.catalog.CommerceCatalogAccess scopedAccess) {
        this.jdbc = jdbc;
        this.enabled = enabled;
        this.selfRegistrationEnabled = selfRegistrationEnabled;
        this.scopedAccess = scopedAccess;
    }
    private void feature() {
        if (!enabled) throw new ApiException(404, "COMMERCE_DISABLED", "Commerce is not enabled.");
    }
    private void access(UUID actor, UUID business, boolean write) {
        feature();
        var roles = jdbc.queryForList("""
            SELECT m.role FROM commerce_memberships m JOIN commerce_businesses b ON b.id=m.business_id
            WHERE m.business_id=? AND m.user_id=? AND m.status='ACTIVE' AND b.status='ACTIVE'
            AND EXISTS(SELECT 1 FROM commerce_entitlements e WHERE e.business_id=b.id AND e.module='CORE' AND e.enabled=true)
            """, String.class, business, actor);
        if (roles.isEmpty()) throw ApiException.notFound();
        if (write && !roles.getFirst().equals("OWNER") && !roles.getFirst().equals("MANAGER"))
            throw new ApiException(403,"COMMERCE_FORBIDDEN","Insufficient commerce permission.");
    }
    private static String label(String name) {
        if (name == null || name.isBlank() || name.length() > 200) throw new IllegalArgumentException("Name must be between 1 and 200 characters.");
        return name.trim();
    }
    public Map<String,Object> create(UUID actor, String name, String currency) {
        feature();
        if (!selfRegistrationEnabled) throw new ApiException(403, "COMMERCE_REGISTRATION_DISABLED", "Commerce self-registration is disabled.");
        name=label(name);
        if(currency==null || !currency.matches("[A-Z]{3}") || !Currency.getAvailableCurrencies().contains(Currency.getInstance(currency)))
            throw new IllegalArgumentException("Invalid ISO currency code.");
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO commerce_businesses(id,name,base_currency) VALUES(?,?,?)",id,name,currency);
        jdbc.update("INSERT INTO commerce_memberships(business_id,user_id,role) VALUES(?,?,'OWNER')",id,actor);
        for(String module: List.of("CORE","INVENTORY","RETAIL","WHOLESALE","DISTRIBUTION","ACCOUNTING"))
            jdbc.update("INSERT INTO commerce_entitlements(business_id,module,enabled) VALUES(?,?,?)",id,module,module.equals("CORE"));
        // Only this newly created tenant receives CORE; existing hotels never receive commerce grants.
        return Map.of("id",id,"name",name,"baseCurrency",currency,"status","ACTIVE");
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> myBusinesses(UUID actor) {
        feature();
        return jdbc.queryForList("""
            SELECT b.id,b.name,b.base_currency,b.status,m.role FROM commerce_businesses b
            JOIN commerce_memberships m ON m.business_id=b.id
            WHERE m.user_id=? AND m.status='ACTIVE' ORDER BY b.created_at,b.id
            """,actor);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> branches(UUID actor,UUID business) {
        access(actor,business,false);
        return jdbc.queryForList("""
          SELECT b.id,b.name,b.contact,b.active FROM commerce_branches b WHERE b.business_id=?
          AND (EXISTS(SELECT 1 FROM commerce_memberships m WHERE m.business_id=b.business_id AND m.user_id=? AND m.role='OWNER' AND m.status='ACTIVE')
          OR EXISTS(SELECT 1 FROM commerce_branch_access a WHERE a.business_id=b.business_id AND a.branch_id=b.id AND a.user_id=?)) ORDER BY b.name,b.id
          """,business,actor,actor);
    }
    public Map<String,Object> addBranch(UUID actor,UUID business,String name) {
        scopedAccess.owner(actor,business);name=label(name);UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO commerce_branches(id,business_id,name) VALUES(?,?,?)",id,business,name);
        return Map.of("id",id,"businessId",business,"name",name,"active",true);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> locations(UUID actor,UUID business,UUID branch) {
        access(actor,business,false);
        scopedAccess.branch(actor,business,branch,"CORE",false);
        if(jdbc.queryForList("SELECT id FROM commerce_branches WHERE business_id=? AND id=?",business,branch).isEmpty()) throw ApiException.notFound();
        return jdbc.queryForList("SELECT id,name,location_type,active FROM commerce_stock_locations WHERE business_id=? AND branch_id=? ORDER BY name,id",business,branch);
    }
    public Map<String,Object> addLocation(UUID actor,UUID business,UUID branch,String name,String type) {
        scopedAccess.owner(actor,business);name=label(name);
        jdbc.queryForObject("SELECT id FROM commerce_businesses WHERE id=? FOR UPDATE",UUID.class,business);
        scopedAccess.branch(actor,business,branch,"CORE");
        if(type==null || !Set.of("SHOP_FLOOR","WAREHOUSE","STOREROOM","RETURNS","DAMAGED").contains(type)) throw new IllegalArgumentException("Invalid location type.");
        if(jdbc.queryForList("SELECT id FROM commerce_branches WHERE business_id=? AND id=? AND active=true",business,branch).isEmpty()) throw ApiException.notFound();
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO commerce_stock_locations(id,business_id,branch_id,name,location_type) VALUES(?,?,?,?,?)",id,business,branch,name,type);
        return Map.of("id",id,"businessId",business,"branchId",branch,"name",name,"type",type,"active",true);
    }
}
