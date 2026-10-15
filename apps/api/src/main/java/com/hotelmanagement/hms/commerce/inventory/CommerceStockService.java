package com.hotelmanagement.hms.commerce.inventory;

import com.hotelmanagement.hms.commerce.catalog.CommerceCatalogAccess;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
public class CommerceStockService {
    private final JdbcTemplate db;
    private final CommerceCatalogAccess access;
    private final com.hotelmanagement.hms.commerce.accounting.CommerceLedgerService ledger;
    public CommerceStockService(JdbcTemplate db,CommerceCatalogAccess access,com.hotelmanagement.hms.commerce.accounting.CommerceLedgerService ledger){this.db=db;this.access=access;this.ledger=ledger;}
    public record Command(UUID requestId,UUID sourceLocation,UUID destinationLocation,UUID productId,BigDecimal quantity,String reason) {}
    public Map<String,Object> post(UUID actor,UUID business,Command command) {
        if(command==null||command.sourceLocation()==null||command.productId()==null) throw new ApiException(400,"INVALID_STOCK_COMMAND","Source location and product are required.");
        if(!access.hasAny(actor,business,"INVENTORY","OWNER","MANAGER","STOREKEEPER")) throw new ApiException(403,"COMMERCE_FORBIDDEN","Stock operation permission required.");
        // Serialize multi-location operations and configuration changes in a stable lock order.
        db.queryForObject("SELECT id FROM commerce_businesses WHERE id=? FOR UPDATE",UUID.class,business);
        if(!access.hasAny(actor,business,"INVENTORY","OWNER","MANAGER","STOREKEEPER")) throw new ApiException(403,"COMMERCE_FORBIDDEN","Stock operation permission required.");
        access.location(actor,business,command.sourceLocation(),"INVENTORY");
        if(command.destinationLocation()!=null) access.location(actor,business,command.destinationLocation(),"INVENTORY");
        String kind=command.destinationLocation()==null?"ADJUSTMENT":"TRANSFER";
        var quantity=command.quantity();
        if(command.requestId()==null||quantity==null||quantity.signum()==0||quantity.scale()>6||quantity.precision()-quantity.scale()>13
                ||command.reason()==null||command.reason().isBlank()||command.reason().length()>500
                ||(kind.equals("TRANSFER")&&(quantity.signum()<0||command.sourceLocation().equals(command.destinationLocation()))))
            throw new ApiException(400,"INVALID_STOCK_COMMAND","A valid quantity, distinct locations and reason are required.");
        var previous=db.queryForList("SELECT * FROM commerce_stock_commands WHERE business_id=? AND request_id=?",business,command.requestId());
        if(!previous.isEmpty()) {
            var p=previous.getFirst();
            if(!actor.equals(p.get("created_by"))||!kind.equals(p.get("kind"))||!command.sourceLocation().equals(p.get("source_location"))
                    ||!Objects.equals(command.destinationLocation(),p.get("destination_location"))||!command.productId().equals(p.get("product_id"))
                    ||quantity.compareTo((BigDecimal)p.get("quantity"))!=0||!command.reason().strip().equals(p.get("reason")))
                throw new ApiException(409,"STOCK_COMMAND_CONFLICT","The request identifier already represents a different stock operation.");
            return Map.of("commandId",p.get("id"),"replayed",true);
        }
        if(db.queryForList("SELECT id FROM commerce_products WHERE business_id=? AND id=? AND active=true",business,command.productId()).isEmpty()) throw ApiException.notFound();
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO commerce_stock_commands(id,business_id,request_id,created_by,kind,source_location,destination_location,product_id,quantity,reason) VALUES(?,?,?,?,?,?,?,?,?,?)",
                id,business,command.requestId(),actor,kind,command.sourceLocation(),command.destinationLocation(),command.productId(),quantity,command.reason().strip());
        BigDecimal transferred=move(actor,business,id,command.sourceLocation(),command.productId(),kind.equals("TRANSFER")?quantity.negate():quantity,kind.equals("TRANSFER")?"TRANSFER_OUT":"ADJUSTMENT",null);
        if(kind.equals("TRANSFER")) move(actor,business,id,command.destinationLocation(),command.productId(),quantity,"TRANSFER_IN",transferred);
        return Map.of("commandId",id,"replayed",false);
    }
    private BigDecimal move(UUID actor,UUID business,UUID command,UUID location,UUID product,BigDecimal delta,String type,BigDecimal incomingValue) {
        db.update("INSERT INTO commerce_stock_balances(business_id,location_id,product_id,quantity) VALUES(?,?,?,0) ON CONFLICT DO NOTHING",business,location,product);
        BigDecimal amount=ledger.value(business,location,product,delta,incomingValue);
        int updated=db.update("UPDATE commerce_stock_balances SET quantity=quantity+? WHERE business_id=? AND location_id=? AND product_id=? AND quantity+?>=0",delta,business,location,product,delta);
        if(updated!=1) throw new ApiException(409,"INSUFFICIENT_STOCK","Insufficient stock at the selected location.");
        db.update("INSERT INTO commerce_stock_movements(id,business_id,location_id,product_id,movement_type,quantity_delta,request_id,created_by,command_id) VALUES(?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID(),business,location,product,type,delta,UUID.randomUUID(),actor,command);
        ledger.stockJournal(actor,business,location,type,command,delta,amount);
        return amount;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> ledger(UUID actor,UUID business,UUID location) {return ledger(actor,business,location,null);}
    public List<Map<String,Object>> ledger(UUID actor,UUID business,UUID location,UUID product) {
        access.location(actor,business,location,"INVENTORY",false);
        return db.queryForList("""
          SELECT m.id,m.product_id,p.name,m.movement_type,m.quantity_delta,m.created_by,m.created_at,COALESCE(c.request_id,f.request_id,m.request_id) AS request_id,c.reason,COALESCE(c.id,f.id) AS command_id
          FROM commerce_stock_movements m JOIN commerce_products p ON p.business_id=m.business_id AND p.id=m.product_id
          LEFT JOIN commerce_stock_commands c ON c.business_id=m.business_id AND c.id=m.command_id
          LEFT JOIN commerce_financial_commands f ON f.business_id=m.business_id AND f.id=m.financial_command_id
          WHERE m.business_id=? AND m.location_id=? AND (?::uuid IS NULL OR m.product_id=?) ORDER BY m.created_at DESC,m.id LIMIT 500
          """,business,location,product,product);
    }
}
