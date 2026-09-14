package com.hotelmanagement.hms.order.service;
import com.hotelmanagement.hms.order.dto.*;
import com.hotelmanagement.hms.order.model.*;
import com.hotelmanagement.hms.order.repository.*;
import com.hotelmanagement.hms.product.repository.ProductRepository;
import com.hotelmanagement.hms.inventory.model.MovementKind;
import com.hotelmanagement.hms.inventory.service.InventoryService;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.*;
import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.folio.service.FolioService;
import com.hotelmanagement.hms.folio.model.EntryKind;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*; import java.math.*;
@Service @Transactional
public class OrderService {
 private final OrderRepository orders; private final OrderItemRepository items;
 private final ProductRepository products; private final InventoryService inventory;
 private final OperationScope scope; private final AuditService audit; private final FolioService folios; private final JdbcTemplate db;
 public OrderService(OrderRepository o,OrderItemRepository i,ProductRepository p,InventoryService n,OperationScope s,AuditService a,FolioService f,JdbcTemplate d){orders=o;items=i;products=p;inventory=n;scope=s;audit=a;folios=f;db=d;}
 public OrderResponse create(UUID h,UUID b,OrderRequest r){
  UUID actor=scope.branch(h,b,ORDER_CREATE);
  if(r.items()==null||r.items().isEmpty()||r.items().size()>100||r.customerId()==null)throw new IllegalArgumentException("Customer and items are required.");
  UUID folio=r.folioId();
  if(folio==null)folio=folios.forCustomer(h,b,r.customerId(),actor).getId();
  if(!folios.lock(h,b,folio).getCustomerId().equals(r.customerId()))throw new IllegalArgumentException("Folio belongs to another customer.");
  if(!Set.of("KITCHEN","BAR","SERVICE").contains(r.destination()))throw new IllegalArgumentException("Invalid destination.");
  var e=orders.saveAndFlush(Order.create(h,b,r.customerId(),folio,actor,r.destination()));
  BigDecimal sub=BigDecimal.ZERO,tax=BigDecimal.ZERO;
  var seen=new HashSet<UUID>();
  for(var x:r.items()){
   if(x==null||x.quantity()==null||x.quantity().signum()<=0||x.quantity().scale()>4||!seen.add(x.productId()))throw new IllegalArgumentException("Invalid or duplicate item.");
   var p=products.findByIdAndHotelId(x.productId(),h).filter(z->z.getActive()&&z.getSellable()).orElseThrow(ApiException::notFound);
   var line=p.getSellingPrice().multiply(x.quantity()).setScale(4,RoundingMode.HALF_UP);
   sub=sub.add(line);tax=tax.add(line.multiply(p.getTaxRate()).setScale(4,RoundingMode.HALF_UP));
   items.save(OrderItem.create(h,b,e.getId(),p.getId(),p.getName(),x.quantity(),p.getSellingPrice(),p.getTaxRate(),p.getDestination().name()));
  }
  e.totals(sub,tax);audit.record(h,b,actor,"ORDER_CREATED","ORDER",e.getId());return OrderResponse.from(e);
 }
 public OrderResponse send(UUID h,UUID b,UUID id){
  UUID actor=scope.branch(h,b,ORDER_SEND);var e=orders.findByIdAndHotelIdAndBranchId(id,h,b).orElseThrow(ApiException::notFound);
  if(e.getStatus()!=OrderStatus.DRAFT)throw new IllegalStateException("Only drafts may be sent.");
  var lines=new ArrayList<>(items.findByOrderIdAndHotelIdAndBranchId(id,h,b));lines.sort(Comparator.comparing(OrderItem::getProductId));
  for(var i:lines){var p=products.lock(i.getProductId(),h).orElseThrow(ApiException::notFound);
   if(p.getStockTracked()){
    var q=i.getQuantity().multiply(p.getSellingFactor()).setScale(4,RoundingMode.HALF_UP);
    inventory.move(h,b,i.getProductId(),q.negate(),MovementKind.SALE,id,actor,"Order sale "+id);
    db.update("update order_items set consumed_quantity=? where order_id=? and product_id=?",q,id,i.getProductId());
   }
  }
  if(e.getTotal().signum()>0)folios.post(h,b,e.getFolioId(),e.getTotal(),EntryKind.ORDER,id,"Order "+id,actor);
  e.send();audit.record(h,b,actor,"ORDER_SENT","ORDER",id);return OrderResponse.from(e);
 }
 public void voidOrder(UUID h,UUID b,UUID id){
  UUID actor=scope.branch(h,b,ORDER_VOID);var e=orders.findByIdAndHotelIdAndBranchId(id,h,b).orElseThrow(ApiException::notFound);
  if(e.getStatus()==OrderStatus.VOIDED)throw new IllegalStateException("Already voided.");
  if(e.getStatus()!=OrderStatus.DRAFT){
   for(var row:db.queryForList("select product_id,consumed_quantity from order_items where order_id=? order by product_id",id)){
    var q=(BigDecimal)row.get("consumed_quantity");
    if(q.signum()>0)inventory.move(h,b,(UUID)row.get("product_id"),q,MovementKind.REVERSAL,id,actor,"Order void "+id);
   }
   if(e.getTotal().signum()>0)folios.post(h,b,e.getFolioId(),e.getTotal().negate(),EntryKind.REVERSAL,id,"Order void "+id,actor);
  }
  e.voidOrder();audit.record(h,b,actor,"ORDER_VOIDED","ORDER",id);
 }
 public OrderResponse advance(UUID h,UUID b,UUID id,OrderStatus next){
  var e=orders.findByIdAndHotelIdAndBranchId(id,h,b).orElseThrow(ApiException::notFound);
  UUID actor;
  if(next==OrderStatus.SERVED)actor=scope.branch(h,b,ORDER_SEND);
  else {
   scope.branch(h,b,ORDER_VIEW);
   throw new IllegalStateException("Update individual preparation items.");
  }
  e.advance(next);audit.record(h,b,actor,"ORDER_"+next,"ORDER",id);return OrderResponse.from(e);
 }
 @Transactional(readOnly=true) public List<Map<String,Object>> queue(UUID h,UUID b,String destination){
  if(!Set.of("BAR","KITCHEN","SERVICE").contains(destination))throw new IllegalArgumentException("Invalid destination.");
  scope.branch(h,b,destination.equals("BAR")?BAR_VIEW:destination.equals("KITCHEN")?KITCHEN_VIEW:ORDER_VIEW);
  return db.queryForList("select i.*,o.created_at,c.name customer_name from order_items i join orders o on o.id=i.order_id join customers c on c.id=o.customer_id where o.hotel_id=? and o.branch_id=? and i.destination=? and o.status in ('SENT','PREPARING','READY') order by o.created_at,i.id",h,b,destination);
 }
 public void prepare(UUID h,UUID b,UUID id,UUID item,OrderStatus next){
  scope.branch(h,b,ORDER_VIEW);
  var order=orders.findByIdAndHotelIdAndBranchId(id,h,b).orElseThrow(ApiException::notFound);
  if(order.getStatus()!=OrderStatus.SENT&&order.getStatus()!=OrderStatus.PREPARING)throw new IllegalStateException("Order cannot be prepared.");
  var rows=db.queryForList("select * from order_items where id=? and order_id=?",item,id);
  if(rows.isEmpty())throw ApiException.notFound();var line=rows.getFirst();String destination=(String)line.get("destination");
  UUID actor=scope.branch(h,b,destination.equals("BAR")?BAR_UPDATE:destination.equals("KITCHEN")?KITCHEN_UPDATE:ORDER_SEND);
  String current=(String)line.get("preparation_status");
  if(!(current.equals("SENT")&&next==OrderStatus.PREPARING||current.equals("PREPARING")&&next==OrderStatus.READY))throw new IllegalStateException("Invalid preparation transition.");
  db.update("update order_items set preparation_status=? where id=?",next.name(),item);
  if(order.getStatus()==OrderStatus.SENT)order.advance(OrderStatus.PREPARING);
  if(db.queryForObject("select count(*) from order_items where order_id=? and preparation_status<>'READY'",Integer.class,id)==0)order.advance(OrderStatus.READY);
  audit.record(h,b,actor,"ITEM_"+next,"ORDER_ITEM",item);
 }
 @Transactional(readOnly=true) public List<Map<String,Object>> list(UUID h,UUID b){
  scope.branch(h,b,ORDER_VIEW);
  return db.queryForList("select o.*,c.name as customer_name from orders o left join customers c on c.id=o.customer_id where o.hotel_id=? and o.branch_id=? order by o.created_at desc limit 100",h,b);
 }
 @Transactional(readOnly=true) public Map<String,Object> detail(UUID h,UUID b,UUID id){
  scope.branch(h,b,ORDER_VIEW);var rows=db.queryForList("select * from orders where id=? and hotel_id=? and branch_id=?",id,h,b);
  if(rows.isEmpty())throw ApiException.notFound();var result=new LinkedHashMap<>(rows.getFirst());
  result.put("items",db.queryForList("select * from order_items where order_id=? order by id",id));return result;
 }
}


