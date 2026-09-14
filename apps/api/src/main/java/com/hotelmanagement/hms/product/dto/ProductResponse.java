package com.hotelmanagement.hms.product.dto;
import com.hotelmanagement.hms.product.model.*;
import java.util.UUID;
import java.math.BigDecimal;
public record ProductResponse(UUID id,String sku,String name,String category,String purchaseUnit,String sellingUnit,String stockUnit,BigDecimal purchaseFactor,BigDecimal sellingFactor,BigDecimal purchasePrice,BigDecimal sellingPrice,BigDecimal taxRate,boolean stockTracked,boolean sellable,boolean purchasable,boolean active,BigDecimal reorderLevel,Destination destination) {
 public static ProductResponse from(Product e){return new ProductResponse(e.getId(),e.getSku(),e.getName(),e.getCategory(),e.getPurchaseUnit(),e.getSellingUnit(),e.getStockUnit(),e.getPurchaseFactor(),e.getSellingFactor(),e.getPurchasePrice(),e.getSellingPrice(),e.getTaxRate(),e.getStockTracked(),e.getSellable(),e.getPurchasable(),e.getActive(),e.getReorderLevel(),e.getDestination());}
}
