package com.hotelmanagement.hms.commerce.inventory;

import com.hotelmanagement.hms.commerce.catalog.CommerceCatalogAccess;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

/** Commerce-only opening inventory; never calls the legacy HMS inventory service. */
@Service
@Transactional
public class CommerceOpeningStockService {
    private final JdbcTemplate jdbc;
    private final CommerceCatalogAccess access;
    private final com.hotelmanagement.hms.commerce.accounting.CommerceLedgerService ledger;
    public CommerceOpeningStockService(JdbcTemplate jdbc,CommerceCatalogAccess access,com.hotelmanagement.hms.commerce.accounting.CommerceLedgerService ledger){this.jdbc=jdbc;this.access=access;this.ledger=ledger;}
    @Transactional(readOnly=true)
    public List<Map<String,Object>> balances(UUID actor,UUID business,UUID location) {
        access.require(actor,business,"INVENTORY",false);
        access.location(actor,business,location,"INVENTORY",false);
        if(jdbc.queryForList("SELECT id FROM commerce_stock_locations WHERE business_id=? AND id=?",business,location).isEmpty()) throw ApiException.notFound();
        return jdbc.queryForList("""
           SELECT s.product_id,p.sku,p.name,p.base_unit,s.quantity FROM commerce_stock_balances s
           JOIN commerce_products p ON p.id=s.product_id AND p.business_id=s.business_id
           WHERE s.business_id=? AND s.location_id=? ORDER BY p.name,p.id
           """,business,location);
    }
    public Map<String,Object> opening(UUID actor,UUID business,UUID location,UUID product,UUID request,BigDecimal quantity) {
        access.require(actor,business,"INVENTORY",true);
        jdbc.queryForObject("SELECT id FROM commerce_businesses WHERE id=? FOR UPDATE",UUID.class,business);
        access.require(actor,business,"INVENTORY",true);
        access.location(actor,business,location,"INVENTORY");
        if(quantity==null || quantity.signum()<=0 || quantity.scale()>6 || quantity.precision()>19)
            throw new ApiException(400,"INVALID_QUANTITY","Opening quantity must be positive, max six decimal places.");
        if(request==null) throw new ApiException(400,"INVALID_REQUEST","Stable requestId is required.");
        // A closed location or inactive item cannot receive opening inventory.
        if(jdbc.queryForList("""
          SELECT l.id FROM commerce_stock_locations l JOIN commerce_branches b
          ON b.id=l.branch_id AND b.business_id=l.business_id
          WHERE l.business_id=? AND l.id=? AND l.active=true AND b.active=true
          """,business,location).isEmpty()) throw ApiException.notFound();
        if(jdbc.queryForList("SELECT id FROM commerce_products WHERE business_id=? AND id=? AND active=true",business,product).isEmpty()) throw ApiException.notFound();
        UUID movementId=UUID.randomUUID();
        // The unique business/request key serializes concurrent retries. Only the first call adds stock.
        int inserted=jdbc.update("""
           INSERT INTO commerce_stock_movements(id,business_id,location_id,product_id,movement_type,quantity_delta,request_id,created_by)
           VALUES(?,?,?,?,'OPENING_STOCK',?,?,?) ON CONFLICT (business_id,request_id) DO NOTHING
           """,movementId,business,location,product,quantity,request,actor);
        if(inserted==0) {
            var original=jdbc.queryForMap("""
                SELECT id,location_id,product_id,quantity_delta,created_by FROM commerce_stock_movements
                WHERE business_id=? AND request_id=?
                """,business,request);
            if(!location.equals(original.get("location_id")) || !product.equals(original.get("product_id")) ||
                ((BigDecimal)original.get("quantity_delta")).compareTo(quantity)!=0 || !actor.equals(original.get("created_by")))
                throw new ApiException(409,"OPENING_STOCK_CONFLICT","Request id was already used with different parameters.");
            return Map.of("movementId",original.get("id"),"requestId",request,"replayed",true);
        }
        BigDecimal carryingValue=ledger.value(business,location,product,quantity,null);
        jdbc.update("""
           INSERT INTO commerce_stock_balances(business_id,location_id,product_id,quantity) VALUES(?,?,?,?)
           ON CONFLICT(business_id,location_id,product_id)
           DO UPDATE SET quantity=commerce_stock_balances.quantity+EXCLUDED.quantity
           """,business,location,product,quantity);
        ledger.stockJournal(actor,business,location,"OPENING_STOCK",movementId,quantity,carryingValue);
        return Map.of("movementId",movementId,"requestId",request,"replayed",false);
    }
}
