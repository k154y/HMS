package com.hotelmanagement.hms.purchase.service;
import com.hotelmanagement.hms.purchase.dto.*;
import com.hotelmanagement.hms.purchase.model.*;
import com.hotelmanagement.hms.purchase.repository.*;
import com.hotelmanagement.hms.inventory.model.MovementKind;
import com.hotelmanagement.hms.inventory.service.InventoryService;
import com.hotelmanagement.hms.vendor.repository.VendorRepository;
import com.hotelmanagement.hms.product.repository.ProductRepository;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.model.Money;
import com.hotelmanagement.hms.shared.web.*;
import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.payment.model.PaymentMethod;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;import java.math.*;
@Service @Transactional
public class PurchaseOrderService {
 private final PurchaseOrderRepository orders;private final PurchaseOrderItemRepository items;
 private final VendorRepository vendors;private final ProductRepository products;
 private final InventoryService inventory;private final OperationScope scope;private final AuditService audit;private final JdbcTemplate db;
 public PurchaseOrderService(PurchaseOrderRepository o,PurchaseOrderItemRepository i,VendorRepository v,ProductRepository p,InventoryService n,OperationScope s,AuditService a,JdbcTemplate d){orders=o;items=i;vendors=v;products=p;inventory=n;scope=s;audit=a;db=d;}
 public PurchaseOrderResponse create(UUID h,UUID b,PurchaseOrderRequest q){
  UUID actor=scope.branch(h,b,PURCHASE_CREATE);vendors.findByIdAndHotelId(q.vendorId(),h).orElseThrow(ApiException::notFound);
  if(q.items()==null||q.items().isEmpty()||q.items().size()>100)throw new IllegalArgumentException("Items are required.");
  BigDecimal total=BigDecimal.ZERO;var seen=new HashSet<UUID>();
  for(var x:q.items()){
   if(x==null||x.quantity()==null||x.unitPrice()==null||x.quantity().scale()>4||x.unitPrice().scale()>4||!seen.add(x.productId()))throw new IllegalArgumentException("Invalid or duplicate purchase item.");
   products.findByIdAndHotelId(x.productId(),h).filter(p->p.getActive()&&p.getPurchasable()).orElseThrow(ApiException::notFound);
   total=total.add(Money.positive(x.quantity()).multiply(Money.nonnegative(x.unitPrice())).setScale(4,RoundingMode.HALF_UP));
  }
  var o=orders.saveAndFlush(PurchaseOrder.create(h,b,q.vendorId(),q.reference(),Money.nonnegative(total),actor));
  q.items().forEach(x->items.save(PurchaseOrderItem.create(h,b,o.getId(),x.productId(),x.quantity(),x.unitPrice())));
  audit.record(h,b,actor,"PURCHASE_CREATED","PURCHASE_ORDER",o.getId());return PurchaseOrderResponse.from(o);
 }
 public PurchaseOrderResponse approve(UUID h,UUID b,UUID id){UUID actor=scope.branch(h,b,PURCHASE_APPROVE);var o=orders.findByIdAndHotelIdAndBranchId(id,h,b).orElseThrow(ApiException::notFound);o.approve();audit.record(h,b,actor,"PURCHASE_APPROVED","PURCHASE_ORDER",id);return PurchaseOrderResponse.from(o);}
 public void receive(UUID h,UUID b,UUID id){
  UUID actor=scope.branch(h,b,PURCHASE_RECEIVE);var o=orders.findByIdAndHotelIdAndBranchId(id,h,b).orElseThrow(ApiException::notFound);
  if(o.getStatus()==PurchaseOrderStatus.RECEIVED)return;
  if(o.getStatus()!=PurchaseOrderStatus.APPROVED)throw new IllegalStateException("Approve purchase before receiving goods.");
  var lines=new ArrayList<>(items.findByPurchaseOrderIdAndHotelIdAndBranchId(id,h,b));lines.sort(Comparator.comparing(PurchaseOrderItem::getProductId));
  for(var i:lines){var p=products.lock(i.getProductId(),h).orElseThrow(ApiException::notFound);var quantity=i.getQuantity().subtract(i.getReceivedQuantity());
   if(quantity.signum()>0){if(p.getStockTracked())inventory.move(h,b,i.getProductId(),quantity.multiply(p.getPurchaseFactor()),MovementKind.RECEIPT,id,actor,"Purchase receipt "+id);i.receive(quantity);}
  }
  o.receive();audit.record(h,b,actor,"PURCHASE_RECEIVED","PURCHASE_ORDER",id);
 }
 @Transactional(readOnly=true) public List<Map<String,Object>> list(UUID h,UUID b){scope.branch(h,b,PURCHASE_VIEW);return db.queryForList("select p.*,v.name vendor_name,coalesce((select sum(amount) from vendor_payments where purchase_order_id=p.id),0) paid from purchase_orders p join vendors v on v.id=p.vendor_id where p.hotel_id=? and p.branch_id=? order by p.created_at desc limit 100",h,b);}
 @Transactional(readOnly=true) public Map<String,Object> detail(UUID h,UUID b,UUID id){scope.branch(h,b,PURCHASE_VIEW);var rows=db.queryForList("select p.*,coalesce((select sum(amount) from vendor_payments where purchase_order_id=p.id),0) paid from purchase_orders p where p.id=? and p.hotel_id=? and p.branch_id=?",id,h,b);if(rows.isEmpty())throw ApiException.notFound();var result=new LinkedHashMap<>(rows.getFirst());result.put("items",db.queryForList("select i.*,p.name from purchase_order_items i join products p on p.id=i.product_id where i.purchase_order_id=?",id));result.put("payments",db.queryForList("select * from vendor_payments where purchase_order_id=? order by created_at",id));return result;}
 public record VendorPayment(BigDecimal amount,PaymentMethod method,UUID requestId){}
 public Map<String,Object> pay(UUID h,UUID b,UUID id,VendorPayment r){
  UUID actor=scope.branch(h,b,PURCHASE_APPROVE);
  if(r.requestId()==null||r.method()==null||r.method()==PaymentMethod.CREDIT)throw new IllegalArgumentException("Payment method and request ID required.");
  var amount=Money.positive(r.amount());var o=orders.findByIdAndHotelIdAndBranchId(id,h,b).orElseThrow(ApiException::notFound);
  var previous=db.queryForList("select * from vendor_payments where hotel_id=? and branch_id=? and request_id=?",h,b,r.requestId());
  if(!previous.isEmpty()){var old=previous.getFirst();if(!id.equals(old.get("purchase_order_id"))||amount.compareTo((BigDecimal)old.get("amount"))!=0||!r.method().name().equals(old.get("method")))throw new IllegalStateException("Payment key reused.");return old;}
  if(o.getStatus()!=PurchaseOrderStatus.APPROVED&&o.getStatus()!=PurchaseOrderStatus.RECEIVED)throw new IllegalStateException("Approve purchase before payment.");
  var paid=db.queryForObject("select coalesce(sum(amount),0) from vendor_payments where purchase_order_id=?",BigDecimal.class,id);
  if(amount.compareTo(o.getTotal().subtract(paid))>0)throw new IllegalStateException("Payment exceeds purchase balance.");
  UUID payment=UUID.randomUUID();db.update("insert into vendor_payments(id,hotel_id,branch_id,purchase_order_id,amount,method,request_id,actor_id) values(?,?,?,?,?,?,?,?)",payment,h,b,id,amount,r.method().name(),r.requestId(),actor);
  audit.record(h,b,actor,"VENDOR_PAYMENT","PURCHASE_ORDER",id);return Map.of("id",payment,"paid",paid.add(amount),"balance",o.getTotal().subtract(paid).subtract(amount));
 }
}
