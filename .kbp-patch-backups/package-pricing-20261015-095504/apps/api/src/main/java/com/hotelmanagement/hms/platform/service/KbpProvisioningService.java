package com.hotelmanagement.hms.platform.service;

import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import com.hotelmanagement.hms.identity.service.UserAccountService;
import com.hotelmanagement.hms.platform.onboarding.dto.OwnerSignupRequest;
import com.hotelmanagement.hms.platform.onboarding.service.OwnerOnboardingService;
import com.hotelmanagement.hms.platform.dto.CreateBranchRequest;
import com.hotelmanagement.hms.shared.web.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class KbpProvisioningService {
    public enum Product { HOTEL, COMMERCE }
    public record CreateOrganization(
            @NotNull Product product,
            @NotBlank @Size(max=200) String organizationName,
            @NotBlank @Email @Size(max=255) String email,
            @NotBlank @Size(max=200) String fullName,
            @NotBlank @Size(min=15,max=128) String temporaryPassword,
            @Pattern(regexp="^(en|fr|rw)$") String preferredLanguage,
            @Pattern(regexp="^[A-Z]{3}$") String currency,
            @Size(max=50) String hotelCode) {
        @Override public String toString(){return "CreateOrganization[REDACTED]";}
    }

    private final JdbcTemplate db;
    private final AuthenticatedUserContext current;
    private final UserAccountService accounts;
    private final OwnerOnboardingService onboarding;
    private final HotelAdministrationService hotels;
    private final boolean commerceEnabled;
    public KbpProvisioningService(JdbcTemplate db, AuthenticatedUserContext current,
            UserAccountService accounts, OwnerOnboardingService onboarding,
            HotelAdministrationService hotels,
            @Value("${hms.commerce.enabled:false}") boolean commerceEnabled) {
        this.db=db;this.current=current;this.accounts=accounts;this.onboarding=onboarding;
        this.hotels=hotels;this.commerceEnabled=commerceEnabled;
    }
    private UUID admin() {
        UUID actor=current.requireCurrentUserId();
        if (!Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM platform_administrators WHERE user_id=?)",
                Boolean.class,actor))) throw new AccessDeniedException("KBP administrator required.");
        return actor;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> organizations() {
        admin();
        return db.queryForList("""
            SELECT id, 'HOTEL' AS product, display_name AS name, status FROM hotels
            UNION ALL
            SELECT id, 'COMMERCE' AS product, name, status FROM commerce_businesses
            ORDER BY product,name LIMIT 500
            """);
    }
    @Transactional
    public Map<String,Object> commerceModules(UUID business,Set<String> modules){
        UUID actor=admin();if(!commerceEnabled)throw ApiException.notFound();
        if(modules==null||!Set.of("CORE","INVENTORY","RETAIL","ACCOUNTING").containsAll(modules)||!modules.containsAll(Set.of("CORE","INVENTORY"))||(modules.contains("RETAIL")&&!modules.contains("ACCOUNTING")))throw new ApiException(400,"INVALID_MODULES","Core and inventory are required; retail also requires accounting.");
        if(db.queryForList("SELECT id FROM commerce_businesses WHERE id=? FOR UPDATE",business).isEmpty())throw ApiException.notFound();
        for(String module:List.of("CORE","INVENTORY","RETAIL","ACCOUNTING"))db.update("INSERT INTO commerce_entitlements(business_id,module,enabled) VALUES(?,?,?) ON CONFLICT(business_id,module) DO UPDATE SET enabled=excluded.enabled",business,module,modules.contains(module));
        UUID owner=db.queryForObject("SELECT user_id FROM commerce_memberships WHERE business_id=? AND role='OWNER' ORDER BY user_id LIMIT 1",UUID.class,business);
        db.update("INSERT INTO kbp_provisioning_audit(id,actor_id,owner_id,product,organization_id,action) VALUES(?,?,?,'COMMERCE',?,?)",UUID.randomUUID(),actor,owner,business,"MODULES_"+String.join("_",new TreeSet<>(modules)));
        return Map.of("updated",true);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> commerceModules(UUID business){admin();return db.queryForList("SELECT module,enabled FROM commerce_entitlements WHERE business_id=? ORDER BY module",business);}
    @Transactional
    public Map<String,Object> create(@Valid CreateOrganization input) {
        UUID actor=admin();
        String language=input.preferredLanguage()==null?"en":input.preferredLanguage();
        String currency=input.currency()==null?"RWF":input.currency();
        UUID ownerId; UUID orgId;
        if(input.product()==Product.HOTEL) {
            if(input.hotelCode()==null||input.hotelCode().isBlank())
                throw new IllegalArgumentException("Hotel code is required.");
            var hotel=new com.hotelmanagement.hms.platform.dto.CreateHotelRequest(
                    input.hotelCode().trim().toUpperCase(Locale.ROOT),
                    input.organizationName().trim(),input.organizationName().trim(),
                    null,null,null,null,currency,"Africa/Kigali",language);
            var result=onboarding.signup(new OwnerSignupRequest(input.email(),
                    input.temporaryPassword(),input.fullName(),null,language,hotel));
            ownerId=result.ownerUserId(); orgId=result.hotel().id();
            hotels.createBranch(ownerId,orgId,
                    new CreateBranchRequest("MAIN","Main branch",null,null,null,null));
        } else {
            if(!commerceEnabled) throw new ApiException(404,"COMMERCE_DISABLED","Commerce is not enabled.");
            if (!Currency.getAvailableCurrencies().contains(Currency.getInstance(currency)))
                throw new IllegalArgumentException("Invalid currency.");
            var owner=accounts.createAccount(input.email(),input.temporaryPassword(),
                    input.fullName(),null,language);
            ownerId=owner.getId(); orgId=UUID.randomUUID();
            db.update("INSERT INTO commerce_businesses(id,name,base_currency) VALUES(?,?,?)",
                    orgId,input.organizationName().trim(),currency);
            db.update("INSERT INTO commerce_memberships(business_id,user_id,role) VALUES(?,?,'OWNER')",
                    orgId,ownerId);
            UUID mainBranch=UUID.randomUUID();
            UUID mainLocation=UUID.randomUUID();
            db.update("INSERT INTO commerce_branches(id,business_id,name) VALUES(?,?,'Main branch')",mainBranch,orgId);
            db.update("INSERT INTO commerce_stock_locations(id,business_id,branch_id,name,location_type) VALUES(?,?,?,'Main warehouse','WAREHOUSE')",
                    mainLocation,orgId,mainBranch);
            for(String module:List.of("CORE","INVENTORY","RETAIL","WHOLESALE","DISTRIBUTION","ACCOUNTING"))
                db.update("INSERT INTO commerce_entitlements(business_id,module,enabled) VALUES(?,?,?)",
                        orgId,module,module.equals("CORE") || module.equals("INVENTORY"));
        }
        db.update("UPDATE users SET must_change_password=TRUE WHERE id=?",ownerId);
        db.update("INSERT INTO kbp_provisioning_audit(id,actor_id,owner_id,product,organization_id,action) VALUES(?,?,?,?,?,?)",
                UUID.randomUUID(),actor,ownerId,input.product().name(),orgId,"OWNER_PROVISIONED");
        return Map.of("ownerUserId",ownerId,"organizationId",orgId,
                "product",input.product().name(),"name",input.organizationName().trim(),
                "mustChangePassword",true);
    }
}
