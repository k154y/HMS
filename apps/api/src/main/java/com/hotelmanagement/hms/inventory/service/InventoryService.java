package com.hotelmanagement.hms.inventory.service;
import com.hotelmanagement.hms.inventory.dto.MovementResponse;
import com.hotelmanagement.hms.inventory.model.*;
import com.hotelmanagement.hms.inventory.repository.StockMovementRepository;
import com.hotelmanagement.hms.product.repository.ProductRepository;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.model.Money;
import com.hotelmanagement.hms.shared.web.*;
import com.hotelmanagement.hms.audit.service.AuditService;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;
import java.math.BigDecimal;
@Service @Transactional
public class InventoryService {
 private final StockMovementRepository movements;private final ProductRepository products;private final OperationScope scope;private final AuditService audit;
 public InventoryService(StockMovementRepository movements,ProductRepository products,OperationScope scope,AuditService audit){this.movements=movements;this.products=products;this.scope=scope;this.audit=audit;}
 // Product locks serialize all movements for that product, including transfers, receipts and sales.
 @Transactional(propagation=Propagation.MANDATORY) public StockMovement move(UUID hotel,UUID branch,UUID product,BigDecimal quantity,MovementKind kind,UUID source,UUID actor,String reason){
  var p=products.lock(product,hotel).orElseThrow(ApiException::notFound);
  if(!p.getStockTracked())throw new IllegalArgumentException("Product is not stock tracked.");
  quantity=Money.amount(quantity);if(quantity.signum()==0 || reason==null || reason.isBlank() || reason.length()>1000)throw new IllegalArgumentException("Invalid stock movement.");
  var existing=movements.findByHotelIdAndBranchIdAndProductIdAndKindAndSourceId(hotel,branch,product,kind,source);
  if(existing.isPresent()){
   if(existing.get().getQuantity().compareTo(quantity)!=0 || !existing.get().getReason().equals(reason))throw new IllegalStateException("Idempotency key reused with different data.");
   return existing.get();
  }
  if(movements.balance(hotel,branch,product).add(quantity).signum()<0)throw new ApiException(409,"INSUFFICIENT_STOCK","Stock cannot become negative.");
  var e=movements.saveAndFlush(StockMovement.create(hotel,branch,product,quantity,p.getStockUnit(),kind,source,actor,reason));
  audit.record(hotel,branch,actor,"STOCK_"+kind,"STOCK_MOVEMENT",e.getId());return e;
 }
 public MovementResponse adjust(UUID hotel,UUID branch,UUID product,BigDecimal quantity,MovementKind kind,UUID request,String reason){
  UUID actor=scope.branch(hotel,branch,INVENTORY_ADJUST);
  if(kind!=MovementKind.OPENING && kind!=MovementKind.ADJUSTMENT && kind!=MovementKind.WASTE)throw new IllegalArgumentException("Invalid adjustment kind.");
  if(kind==MovementKind.OPENING && quantity.signum()<=0 || kind==MovementKind.WASTE && quantity.signum()>=0)throw new IllegalArgumentException("Invalid movement sign.");
  return MovementResponse.from(move(hotel,branch,product,quantity,kind,request,actor,reason));
 }
 public void transfer(UUID hotel,UUID sourceBranch,UUID targetBranch,UUID product,BigDecimal quantity,UUID request,String reason){
  UUID actor=scope.branch(hotel,sourceBranch,INVENTORY_TRANSFER);scope.branch(hotel,targetBranch,INVENTORY_TRANSFER);
  if(sourceBranch.equals(targetBranch))throw new IllegalArgumentException("Same branch.");
  quantity=Money.positive(quantity);
  move(hotel,sourceBranch,product,quantity.negate(),MovementKind.TRANSFER_OUT,request,actor,reason);
  move(hotel,targetBranch,product,quantity,MovementKind.TRANSFER_IN,request,actor,reason);
 }
 public MovementResponse reverse(UUID hotel,UUID branch,UUID id,String reason){
  UUID actor=scope.branch(hotel,branch,INVENTORY_ADJUST);var m=movements.findByIdAndHotelIdAndBranchId(id,hotel,branch).orElseThrow(ApiException::notFound);
  if(m.getKind()!=MovementKind.ADJUSTMENT && m.getKind()!=MovementKind.OPENING && m.getKind()!=MovementKind.WASTE)throw new IllegalStateException("Reverse through originating operation.");
  return MovementResponse.from(move(hotel,branch,m.getProductId(),m.getQuantity().negate(),MovementKind.REVERSAL,id,actor,reason));
 }
 public record StockResponse(UUID productId,BigDecimal quantity,String unit,BigDecimal reorderLevel,boolean lowStock){}
 @Transactional(readOnly=true) public StockResponse stock(UUID hotel,UUID branch,UUID product){
  scope.branch(hotel,branch,INVENTORY_VIEW);var p=products.findByIdAndHotelId(product,hotel).orElseThrow(ApiException::notFound);var quantity=movements.balance(hotel,branch,product);
  return new StockResponse(product,quantity,p.getStockUnit(),p.getReorderLevel(),quantity.compareTo(p.getReorderLevel())<=0);
 }
 @Transactional(readOnly=true) public PageResponse<MovementResponse> history(UUID hotel,UUID branch,UUID product,int page,int size){
  scope.branch(hotel,branch,INVENTORY_VIEW);products.findByIdAndHotelId(product,hotel).orElseThrow(ApiException::notFound);
  return PageResponse.from(movements.findByHotelIdAndBranchIdAndProductId(hotel,branch,product,scope.page(page,size)).map(MovementResponse::from));
 }
}
