package com.hotelmanagement.hms.commerce.catalog;

import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.*;

/** Explicit business membership AND module entitlement. Hotel roles do not grant commerce privileges. */
@Component
public class CommerceCatalogAccess {
    private final JdbcTemplate jdbc;
    private final boolean enabled;
    public CommerceCatalogAccess(JdbcTemplate jdbc, @Value("${hms.commerce.enabled:false}") boolean enabled) {this.jdbc=jdbc;this.enabled=enabled;}
    public void require(UUID actor,UUID business,String module,boolean write) {
        String role=role(actor,business,module);
        if(write && !Set.of("OWNER","MANAGER").contains(role)) throw new ApiException(403,"COMMERCE_FORBIDDEN","Insufficient commerce permission.");
    }
    public String role(UUID actor,UUID business,String module) {
        if(!enabled) throw ApiException.notFound();
        List<String> roles=jdbc.queryForList("""
          SELECT m.role FROM commerce_memberships m JOIN commerce_businesses b ON b.id=m.business_id
          JOIN commerce_entitlements core ON core.business_id=b.id AND core.module='CORE' AND core.enabled=true
          WHERE m.user_id=? AND m.business_id=? AND m.status='ACTIVE' AND b.status='ACTIVE'
          AND (?='CORE' OR EXISTS(SELECT 1 FROM commerce_entitlements e WHERE e.business_id=b.id AND e.module=? AND e.enabled=true))
          """,String.class,actor,business,module,module);
        if(roles.isEmpty()) throw ApiException.notFound();
        return roles.getFirst();
    }
    public void owner(UUID actor,UUID business) {
        if(!role(actor,business,"CORE").equals("OWNER")) throw new ApiException(403,"COMMERCE_FORBIDDEN","Business owner permission required.");
    }
    public boolean financial(UUID actor,UUID business) {
        return Set.of("OWNER","MANAGER","ACCOUNTANT").contains(role(actor,business,"CORE"));
    }
    public void branch(UUID actor,UUID business,UUID branch,String module) {
        branch(actor,business,branch,module,true);
    }
    public void branch(UUID actor,UUID business,UUID branch,String module,boolean requireActive) {
        String role=role(actor,business,module);
        if(jdbc.queryForList("SELECT id FROM commerce_branches WHERE business_id=? AND id=? AND (NOT ? OR active=true)",business,branch,requireActive).isEmpty()) throw ApiException.notFound();
        if(!role.equals("OWNER") && jdbc.queryForList("SELECT branch_id FROM commerce_branch_access WHERE business_id=? AND user_id=? AND branch_id=?",business,actor,branch).isEmpty()) throw ApiException.notFound();
    }
    public UUID location(UUID actor,UUID business,UUID location,String module) {
        return location(actor,business,location,module,true);
    }
    public UUID location(UUID actor,UUID business,UUID location,String module,boolean requireActive) {
        role(actor,business,module);
        var rows=jdbc.queryForList("SELECT branch_id FROM commerce_stock_locations WHERE business_id=? AND id=? AND (NOT ? OR active=true)",UUID.class,business,location,requireActive);
        if(rows.isEmpty()) throw ApiException.notFound();
        branch(actor,business,rows.getFirst(),module,requireActive);
        return rows.getFirst();
    }
}
