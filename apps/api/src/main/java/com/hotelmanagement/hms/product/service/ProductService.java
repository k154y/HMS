package com.hotelmanagement.hms.product.service;
import com.hotelmanagement.hms.product.dto.*;
import com.hotelmanagement.hms.product.model.*;
import com.hotelmanagement.hms.product.repository.*;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.*;
import com.hotelmanagement.hms.audit.service.AuditService;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @Transactional
public class ProductService {
 private final ProductRepository repository;private final OperationScope scope;private final AuditService audit; private final org.springframework.jdbc.core.JdbcTemplate db;
 public ProductService(ProductRepository repository,OperationScope scope,AuditService audit,org.springframework.jdbc.core.JdbcTemplate db){this.repository=repository;this.scope=scope;this.audit=audit;this.db=db;}
 public ProductResponse create(UUID hotel,ProductRequest r){
  UUID actor=scope.hotel(hotel,PRODUCT_MANAGE);var e=repository.saveAndFlush(Product.create(hotel,r.sku(),r.name(),r.category(),r.purchaseUnit(),r.sellingUnit(),r.stockUnit(),r.purchaseFactor(),r.sellingFactor(),r.purchasePrice(),r.sellingPrice(),r.taxRate(),r.stockTracked(),r.sellable(),r.purchasable(),r.active(),r.reorderLevel(),r.destination()));
  audit.record(hotel,null,actor,"PRODUCT_CREATED","PRODUCT",e.getId());return ProductResponse.from(e);
 }
 public ProductResponse update(UUID hotel,UUID id,ProductRequest r){
  UUID actor=scope.hotel(hotel,PRODUCT_MANAGE);var product=repository.lock(id,hotel).orElseThrow(ApiException::notFound);
  boolean unitsChanged=!product.getPurchaseUnit().equals(r.purchaseUnit())||!product.getSellingUnit().equals(r.sellingUnit())||!product.getStockUnit().equals(r.stockUnit())||product.getPurchaseFactor().compareTo(r.purchaseFactor())!=0||product.getSellingFactor().compareTo(r.sellingFactor())!=0||product.getStockTracked()!=r.stockTracked();
  if(unitsChanged&&Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from stock_movements where product_id=?) or exists(select 1 from order_items where product_id=?) or exists(select 1 from purchase_order_items where product_id=?)",Boolean.class,id,id,id)))throw new IllegalStateException("Units cannot change after stock or sales history exists.");
  product.edit(r);audit.record(hotel,null,actor,"PRODUCT_UPDATED","PRODUCT",id);return ProductResponse.from(product);
 }
 @Transactional(readOnly=true) public ProductResponse get(UUID hotel,UUID id){scope.hotel(hotel,PRODUCT_VIEW);return ProductResponse.from(repository.findByIdAndHotelId(id,hotel).orElseThrow(ApiException::notFound));}
 @Transactional(readOnly=true) public PageResponse<ProductResponse> list(UUID hotel,int page,int size){scope.hotel(hotel,PRODUCT_VIEW);return PageResponse.from(repository.findByHotelId(hotel,scope.page(page,size)).map(ProductResponse::from));}
}

