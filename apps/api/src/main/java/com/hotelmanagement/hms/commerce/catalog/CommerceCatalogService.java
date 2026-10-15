package com.hotelmanagement.hms.commerce.catalog;

import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
public class CommerceCatalogService {
    private final JdbcTemplate jdbc;
    private final CommerceCatalogAccess access;
    public CommerceCatalogService(JdbcTemplate jdbc, CommerceCatalogAccess access) {this.jdbc=jdbc;this.access=access;}
    private String required(String value, int max) {
        if (value == null || value.isBlank() || value.strip().length()>max) throw new ApiException(400,"INVALID_FIELD","Required text must be 1 to "+max+" characters.");
        return value.strip();
    }
    private BigDecimal money(BigDecimal value) {
        if(value==null || value.signum()<0 || value.scale()>4 || value.precision()>19) throw new ApiException(400,"INVALID_AMOUNT","Invalid nonnegative amount (max four decimals).");
        return value;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> categories(UUID user,UUID business) {
        access.require(user,business,"CORE",false);
        return jdbc.queryForList("SELECT id,parent_id,name,active FROM commerce_categories WHERE business_id=? ORDER BY name,id",business);
    }
    public Map<String,Object> addCategory(UUID user,UUID business,String name,UUID parent) {
        access.require(user,business,"CORE",true); name=required(name,160);
        jdbc.queryForObject("SELECT id FROM commerce_businesses WHERE id=? FOR UPDATE",UUID.class,business);
        access.require(user,business,"CORE",true);
        if(parent!=null && jdbc.queryForList("SELECT id FROM commerce_categories WHERE business_id=? AND id=? AND active=true",business,parent).isEmpty()) throw ApiException.notFound();
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO commerce_categories(id,business_id,parent_id,name) VALUES(?,?,?,?)",id,business,parent,name);
        Map<String,Object> out=new LinkedHashMap<>();out.put("id",id);out.put("parentId",parent);out.put("name",name);return out;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> products(UUID user,UUID business) {
        access.require(user,business,"CORE",false);
        var rows=jdbc.queryForList("SELECT * FROM commerce_products WHERE business_id=? ORDER BY name,id",business);
        if(!access.financial(user,business)) rows.forEach(row->row.remove("purchase_cost"));
        return rows;
    }
    public Map<String,Object> addProduct(UUID user,UUID business,UUID category,String sku,String barcode,String name,String baseUnit,BigDecimal cost,BigDecimal retail,BigDecimal wholesale) {
        access.require(user,business,"CORE",true);
        jdbc.queryForObject("SELECT id FROM commerce_businesses WHERE id=? FOR UPDATE",UUID.class,business);
        access.require(user,business,"CORE",true);
        sku=required(sku,80);name=required(name,200);baseUnit=required(baseUnit,32);
        if(barcode!=null) {barcode=barcode.strip();if(barcode.isEmpty()) barcode=null; else if(barcode.length()>120) throw new ApiException(400,"INVALID_BARCODE","Barcode is too long.");}
        if(category!=null && jdbc.queryForList("SELECT id FROM commerce_categories WHERE business_id=? AND id=? AND active=true",business,category).isEmpty()) throw ApiException.notFound();
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO commerce_products(id,business_id,category_id,sku,barcode,name,base_unit,purchase_cost,retail_price,wholesale_price) VALUES(?,?,?,?,?,?,?,?,?,?)",id,business,category,sku,barcode,name,baseUnit,money(cost),money(retail),wholesale==null?null:money(wholesale));
        jdbc.update("INSERT INTO commerce_product_units(business_id,product_id,unit_name,units_per_package) VALUES(?,?,?,1)",business,id,baseUnit);
        return Map.of("id",id,"sku",sku,"name",name,"baseUnit",baseUnit);
    }
    public Map<String,Object> addUnit(UUID user,UUID business,UUID product,String unit,BigDecimal factor) {
        access.require(user,business,"CORE",true);
        unit=required(unit,32);
        if(factor==null || factor.signum()<=0 || factor.scale()>6 || factor.precision()>19) throw new ApiException(400,"INVALID_FACTOR","Conversion factor must be positive with max six decimals.");
        if(jdbc.queryForList("SELECT id FROM commerce_products WHERE business_id=? AND id=? AND active=true",business,product).isEmpty()) throw ApiException.notFound();
        jdbc.update("INSERT INTO commerce_product_units(business_id,product_id,unit_name,units_per_package) VALUES(?,?,?,?)",business,product,unit,factor);
        return Map.of("productId",product,"unit",unit,"factor",factor);
    }
    public record ProductUpdate(String name,String description,UUID categoryId,String barcode,BigDecimal purchaseCost,
                                BigDecimal retailPrice,BigDecimal wholesalePrice,BigDecimal minimumPrice,BigDecimal taxRate,
                                boolean taxInclusive,boolean purchaseTaxRecoverable,BigDecimal lowStockQuantity,boolean active,
                                Boolean discountEnabled,BigDecimal cashierDiscountLimit,Integer returnWindowDays,Boolean fractionalUnits) {
        public ProductUpdate(String name,String description,UUID categoryId,String barcode,BigDecimal purchaseCost,
                             BigDecimal retailPrice,BigDecimal wholesalePrice,BigDecimal minimumPrice,BigDecimal taxRate,
                             boolean taxInclusive,boolean purchaseTaxRecoverable,BigDecimal lowStockQuantity,boolean active){
            this(name,description,categoryId,barcode,purchaseCost,retailPrice,wholesalePrice,minimumPrice,taxRate,
                 taxInclusive,purchaseTaxRecoverable,lowStockQuantity,active,null,null,null,null);
        }
    }
    public void deactivateProduct(UUID actor,UUID business,UUID product){
        access.require(actor,business,"CORE",true);
        jdbc.queryForObject("SELECT id FROM commerce_businesses WHERE id=? FOR UPDATE",UUID.class,business);
        access.require(actor,business,"CORE",true);
        var active=jdbc.queryForList("SELECT active FROM commerce_products WHERE business_id=? AND id=?",Boolean.class,business,product);
        if(active.isEmpty())throw ApiException.notFound();
        if(!active.getFirst())return;
        auditProduct(actor,business,product,"PRODUCT_BEFORE_UPDATE");
        jdbc.update("UPDATE commerce_products SET active=false WHERE business_id=? AND id=?",business,product);
        auditProduct(actor,business,product,"PRODUCT_UPDATED");
    }
    public void updateProduct(UUID actor,UUID business,UUID product,ProductUpdate p){
        access.require(actor,business,"CORE",true);jdbc.queryForObject("SELECT id FROM commerce_businesses WHERE id=? FOR UPDATE",UUID.class,business);access.require(actor,business,"CORE",true);
        String name=required(p.name(),200);
        if(p.description()!=null&&p.description().length()>2000)throw new ApiException(400,"INVALID_FIELD","Description is too long.");
        if(p.barcode()!=null&&p.barcode().length()>120)throw new ApiException(400,"INVALID_FIELD","Barcode is too long.");
        money(p.minimumPrice());money(p.purchaseCost());money(p.retailPrice());if(p.wholesalePrice()!=null)money(p.wholesalePrice());money(p.taxRate());
        if(p.taxRate().compareTo(new BigDecimal("100"))>0||p.retailPrice().compareTo(p.minimumPrice())<0||(p.wholesalePrice()!=null&&p.wholesalePrice().compareTo(p.minimumPrice())<0))throw new ApiException(400,"INVALID_PRICE","Invalid tax or minimum selling price.");
        if(p.lowStockQuantity()==null||p.lowStockQuantity().signum()<0||p.lowStockQuantity().scale()>6||p.lowStockQuantity().precision()-p.lowStockQuantity().scale()>13)throw new ApiException(400,"INVALID_QUANTITY","Invalid stock threshold.");
        if(p.cashierDiscountLimit()!=null&&(p.cashierDiscountLimit().signum()<0||p.cashierDiscountLimit().compareTo(new BigDecimal("100"))>0||p.cashierDiscountLimit().scale()>4))
            throw new ApiException(400,"INVALID_DISCOUNT_POLICY","Cashier discount must be between zero and 100 percent.");
        if(p.returnWindowDays()!=null&&(p.returnWindowDays()<0||p.returnWindowDays()>3650))
            throw new ApiException(400,"INVALID_RETURN_WINDOW","Return days must be between 0 and 3650.");
        if(p.categoryId()!=null&&jdbc.queryForList("SELECT id FROM commerce_categories WHERE business_id=? AND id=? AND active",business,p.categoryId()).isEmpty())throw ApiException.notFound();
        if(jdbc.queryForList("SELECT id FROM commerce_products WHERE business_id=? AND id=?",business,product).isEmpty())throw ApiException.notFound();
        auditProduct(actor,business,product,"PRODUCT_BEFORE_UPDATE");
        jdbc.update("UPDATE commerce_products SET name=?,description=?,category_id=?,barcode=?,purchase_cost=?,retail_price=?,wholesale_price=?,minimum_price=?,tax_rate=?,tax_inclusive=?,purchase_tax_recoverable=?,low_stock_quantity=?,active=?,discount_enabled=COALESCE(?,discount_enabled),cashier_discount_limit=COALESCE(?,cashier_discount_limit),return_window_days=COALESCE(?,return_window_days),fractional_units=COALESCE(?,fractional_units) WHERE business_id=? AND id=?",name,p.description(),p.categoryId(),p.barcode()==null||p.barcode().isBlank()?null:p.barcode().strip(),p.purchaseCost(),p.retailPrice(),p.wholesalePrice(),p.minimumPrice(),p.taxRate(),p.taxInclusive(),p.purchaseTaxRecoverable(),p.lowStockQuantity(),p.active(),p.discountEnabled(),p.cashierDiscountLimit(),p.returnWindowDays(),p.fractionalUnits(),business,product);
        auditProduct(actor,business,product,"PRODUCT_UPDATED");
    }
    public Map<String,Object> createConfigured(UUID actor,UUID business,CommerceCatalogController.NewProduct input) {
        var product=input.product();
        var created=addProduct(actor,business,product.categoryId(),input.sku(),product.barcode(),product.name(),input.baseUnit(),product.purchaseCost(),product.retailPrice(),product.wholesalePrice());
        configureProduct(actor,business,(UUID)created.get("id"),product,input.packageSetting(),input.packageSettings(),input.branchPrice());
        return created;
    }
    public void configureProduct(UUID actor,UUID business,UUID product,ProductUpdate update,
            CommerceCatalogController.PackageSetting pack,CommerceCatalogController.BranchPrice branch){
        configureProduct(actor,business,product,update,pack,null,branch);
    }
    public void configureProduct(UUID actor,UUID business,UUID product,ProductUpdate update,
            CommerceCatalogController.PackageSetting pack,List<CommerceCatalogController.PackageSetting> packs,
            CommerceCatalogController.BranchPrice branch){
        // One transaction covers product, any number of selling units and branch prices.
        if(pack!=null&&packs!=null&&!packs.isEmpty())throw new ApiException(400,"AMBIGUOUS_UNITS","Submit packageSetting or packageSettings, not both.");
        var all=packs!=null?packs:(pack!=null?List.of(pack):List.<CommerceCatalogController.PackageSetting>of());
        if(all.size()>50)throw new ApiException(400,"TOO_MANY_UNITS","Use at most 50 package units per product.");
        var names=new HashSet<String>();
        String baseUnit=jdbc.queryForObject("SELECT base_unit FROM commerce_products WHERE business_id=? AND id=?",String.class,business,product);
        if(baseUnit==null)throw ApiException.notFound();
        for(var unit:all){
            if(unit==null||unit.name()==null||unit.name().isBlank()||unit.name().strip().equalsIgnoreCase(baseUnit)||
               !names.add(unit.name().strip().toLowerCase(Locale.ROOT)))
                throw new ApiException(400,"DUPLICATE_UNIT","Package names must be unique and different from the stock base unit.");
        }
        updateProduct(actor,business,product,update);
        for(var packToSave:all){
            String unit=required(packToSave.name(),32);
            BigDecimal factor=packToSave.factor();
            if(factor==null||factor.signum()<=0||factor.scale()>6||factor.precision()>19)
                throw new ApiException(400,"INVALID_FACTOR","Invalid package conversion factor.");
            if(packToSave.retailPackagePrice()!=null)money(packToSave.retailPackagePrice());
            if(packToSave.wholesalePackagePrice()!=null)money(packToSave.wholesalePackagePrice());
            if(packToSave.purchasePackagePrice()!=null)money(packToSave.purchasePackagePrice());
            BigDecimal minimum=update.minimumPrice().multiply(factor);
            if(packToSave.retailPackagePrice()!=null&&packToSave.retailPackagePrice().compareTo(minimum)<0||
               packToSave.wholesalePackagePrice()!=null&&packToSave.wholesalePackagePrice().compareTo(minimum)<0)
                throw new ApiException(400,"INVALID_PRICE","Package selling price is below the authorized minimum.");
            var old=jdbc.queryForList("SELECT units_per_package FROM commerce_product_units WHERE business_id=? AND product_id=? AND unit_name=?",BigDecimal.class,business,product,unit);
            // Conversions used on posted documents cannot be silently redefined.
            if(!old.isEmpty()&&old.getFirst().compareTo(factor)!=0&&Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM commerce_document_lines l JOIN commerce_documents d ON d.id=l.document_id AND d.business_id=l.business_id WHERE l.business_id=? AND l.product_id=? AND l.unit=?)",Boolean.class,business,product,unit)))
                throw new ApiException(409,"UNIT_IN_USE","Create a new package name rather than changing a historically used conversion.");
            jdbc.update("""
                INSERT INTO commerce_product_units(business_id,product_id,unit_name,units_per_package,retail_package_price,wholesale_package_price,purchase_package_price)
                VALUES(?,?,?,?,?,?,?) ON CONFLICT(business_id,product_id,unit_name)
                DO UPDATE SET units_per_package=excluded.units_per_package,retail_package_price=excluded.retail_package_price,
                wholesale_package_price=excluded.wholesale_package_price,purchase_package_price=excluded.purchase_package_price
                """,business,product,unit,factor,packToSave.retailPackagePrice(),packToSave.wholesalePackagePrice(),packToSave.purchasePackagePrice());
        }
        if(branch!=null)branchPrice(actor,business,product,branch.branchId(),branch.price(),branch.effectiveAt());
    }
    private void auditProduct(UUID actor,UUID business,UUID product,String action){jdbc.update("INSERT INTO commerce_configuration_audit(id,business_id,actor_id,action,entity_id,detail) SELECT ?,business_id,?,?,id,to_jsonb(p) FROM commerce_products p WHERE business_id=? AND id=?",UUID.randomUUID(),actor,action,business,product);}
    public void branchPrice(UUID actor,UUID business,UUID product,UUID branch,BigDecimal price,java.time.OffsetDateTime effective){
        access.require(actor,business,"CORE",true);jdbc.queryForObject("SELECT id FROM commerce_businesses WHERE id=? FOR UPDATE",UUID.class,business);access.require(actor,business,"CORE",true);access.branch(actor,business,branch,"CORE");money(price);
        if(effective==null||effective.isBefore(java.time.OffsetDateTime.now().minusMinutes(5)))throw new ApiException(400,"INVALID_DATE","New branch prices cannot be backdated.");
        var rows=jdbc.queryForList("SELECT minimum_price FROM commerce_products WHERE business_id=? AND id=? AND active",BigDecimal.class,business,product);if(rows.isEmpty())throw ApiException.notFound();
        if(price.compareTo(rows.getFirst())<0)throw new ApiException(400,"INVALID_PRICE","Branch price is below the minimum.");
        jdbc.update("INSERT INTO commerce_branch_prices(id,business_id,branch_id,product_id,price,effective_at,created_by) VALUES(?,?,?,?,?,?,?)",UUID.randomUUID(),business,branch,product,price,effective,actor);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> units(UUID user,UUID business,UUID product) {
        access.require(user,business,"CORE",false);
        var result=jdbc.queryForList("SELECT unit_name,units_per_package,retail_package_price,wholesale_package_price,purchase_package_price FROM commerce_product_units WHERE business_id=? AND product_id=? ORDER BY unit_name",business,product);
        if(!access.financial(user,business))result.forEach(unit->unit.remove("purchase_package_price"));
        return result;
    }
}
