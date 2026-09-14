package com.hotelmanagement.hms.product.model;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="products")
public class Product extends com.hotelmanagement.hms.shared.model.HotelEntity {
    @Column(name="sku") private String sku;
    @Column(name="name") private String name;
    @Column(name="category") private String category;
    @Column(name="purchase_unit") private String purchaseUnit;
    @Column(name="selling_unit") private String sellingUnit;
    @Column(name="stock_unit") private String stockUnit;
    @Column(name="purchase_factor",precision=19,scale=4) private BigDecimal purchaseFactor;
    @Column(name="selling_factor",precision=19,scale=4) private BigDecimal sellingFactor;
    @Column(name="purchase_price",precision=19,scale=4) private BigDecimal purchasePrice;
    @Column(name="selling_price",precision=19,scale=4) private BigDecimal sellingPrice;
    @Column(name="tax_rate",precision=19,scale=4) private BigDecimal taxRate;
    @Column(name="stock_tracked") private boolean stockTracked;
    @Column(name="sellable") private boolean sellable;
    @Column(name="purchasable") private boolean purchasable;
    @Column(name="active") private boolean active;
    @Column(name="reorder_level",precision=19,scale=4) private BigDecimal reorderLevel;
    @Enumerated(EnumType.STRING)
    @Column(name="destination") private Destination destination;
    protected Product() {}
    public static Product create(UUID hotelId, String sku, String name, String category, String purchaseUnit, String sellingUnit, String stockUnit, BigDecimal purchaseFactor, BigDecimal sellingFactor, BigDecimal purchasePrice, BigDecimal sellingPrice, BigDecimal taxRate, boolean stockTracked, boolean sellable, boolean purchasable, boolean active, BigDecimal reorderLevel, Destination destination) {
        var e=new Product();
        e.hotelId=hotelId;
        e.sku=sku;
        e.name=name;
        e.category=category;
        e.purchaseUnit=purchaseUnit;
        e.sellingUnit=sellingUnit;
        e.stockUnit=stockUnit;
        e.purchaseFactor=purchaseFactor;
        e.sellingFactor=sellingFactor;
        e.purchasePrice=purchasePrice;
        e.sellingPrice=sellingPrice;
        e.taxRate=taxRate;
        e.stockTracked=stockTracked;
        e.sellable=sellable;
        e.purchasable=purchasable;
        e.active=active;
        e.reorderLevel=reorderLevel;
        e.destination=destination;
        return e;
    }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getPurchaseUnit() { return purchaseUnit; }
    public String getSellingUnit() { return sellingUnit; }
    public String getStockUnit() { return stockUnit; }
    public BigDecimal getPurchaseFactor() { return purchaseFactor; }
    public BigDecimal getSellingFactor() { return sellingFactor; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public BigDecimal getTaxRate() { return taxRate; }
    public boolean getStockTracked() { return stockTracked; }
    public boolean getSellable() { return sellable; }
    public boolean getPurchasable() { return purchasable; }
    public boolean getActive() { return active; }
    public BigDecimal getReorderLevel() { return reorderLevel; }
    public Destination getDestination() { return destination; }
 public void prices(BigDecimal purchase,BigDecimal selling,BigDecimal tax) {
  purchasePrice=com.hotelmanagement.hms.shared.model.Money.nonnegative(purchase);sellingPrice=com.hotelmanagement.hms.shared.model.Money.nonnegative(selling);
  if(tax==null || tax.signum()<0 || tax.compareTo(BigDecimal.ONE)>0)throw new IllegalArgumentException("Invalid tax rate.");taxRate=tax;
 }
 public void edit(com.hotelmanagement.hms.product.dto.ProductRequest r){sku=r.sku();name=r.name();category=r.category();purchaseUnit=r.purchaseUnit();sellingUnit=r.sellingUnit();stockUnit=r.stockUnit();purchaseFactor=r.purchaseFactor();sellingFactor=r.sellingFactor();prices(r.purchasePrice(),r.sellingPrice(),r.taxRate());stockTracked=r.stockTracked();sellable=r.sellable();purchasable=r.purchasable();active=r.active();reorderLevel=r.reorderLevel();destination=r.destination();}
 public void activate(boolean value){active=value;}
}

