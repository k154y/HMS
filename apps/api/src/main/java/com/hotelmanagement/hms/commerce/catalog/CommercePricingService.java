package com.hotelmanagement.hms.commerce.catalog;

import com.hotelmanagement.hms.commerce.foundation.CommerceConfigurationService;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.OffsetDateTime;
import java.util.*;
import static com.hotelmanagement.hms.commerce.catalog.CommerceCatalogController.*;

@Service
@Transactional
public class CommercePricingService {
    private final JdbcTemplate db;
    private final CommerceCatalogAccess access;
    private final CommerceCatalogService catalog;
    private final CommerceConfigurationService currencies;
    public CommercePricingService(JdbcTemplate db,CommerceCatalogAccess access,CommerceCatalogService catalog,CommerceConfigurationService currencies){this.db=db;this.access=access;this.catalog=catalog;this.currencies=currencies;}
    public record Settings(String currency,String baseCurrency,BigDecimal rateToBase,int decimals) {}
    private void lock(UUID business){db.queryForObject("SELECT id FROM commerce_businesses WHERE id=? FOR UPDATE",UUID.class,business);}
    private String current(UUID business){return db.queryForObject("SELECT COALESCE(default_transaction_currency,base_currency) FROM commerce_businesses WHERE id=?",String.class,business);}
    private Settings quote(UUID actor,UUID business,String currency){
        var q=currencies.quote(actor,business,currency,BigDecimal.ZERO,OffsetDateTime.now());
        return new Settings(currency,q.baseCurrency(),q.rateToBase(),Currency.getInstance(currency).getDefaultFractionDigits());
    }
    @Transactional(readOnly=true)
    public Settings settings(UUID actor,UUID business){access.require(actor,business,"CORE",false);return quote(actor,business,current(business));}
    private List<Map<String,Object>> converted(List<Map<String,Object>> rows,Settings s,String... keys){
        return rows.stream().map(row->{var copy=new LinkedHashMap<>(row);for(String key:keys)if(copy.get(key)!=null){
            var amount=new BigDecimal(copy.get(key).toString());
            copy.put(key,amount.divide(s.rateToBase(),s.decimals(),key.equals("minimum_price")?RoundingMode.CEILING:RoundingMode.HALF_UP));
        }return (Map<String,Object>)copy;}).toList();
    }
    @Transactional(readOnly=true)
    public Map<String,Object> preview(UUID actor,UUID business,String currency){
        access.owner(actor,business);var target=quote(actor,business,currency);
        return Map.of("settings",target,"previousCurrency",current(business),"products",products(actor,business,target));
    }
    private List<Map<String,Object>> products(UUID actor,UUID business,Settings s){return converted(catalog.products(actor,business),s,"purchase_cost","retail_price","wholesale_price","minimum_price");}
    @Transactional(readOnly=true)
    public Map<String,Object> catalog(UUID actor,UUID business){var s=settings(actor,business);return Map.of("settings",s,"products",products(actor,business,s));}
    @Transactional(readOnly=true)
    public List<Map<String,Object>> units(UUID actor,UUID business,UUID product){return converted(catalog.units(actor,business,product),settings(actor,business),"purchase_package_price","retail_package_price","wholesale_package_price");}
    public Settings choose(UUID actor,UUID business,String code,String previous,BigDecimal expectedRate){
        access.owner(actor,business);lock(business);access.owner(actor,business);
        var s=quote(actor,business,code);
        if(expectedRate==null||s.rateToBase().compareTo(expectedRate)!=0)throw stale();
        String old=current(business);
        if(old.equals(code))return s;
        if(!old.equals(previous))throw stale();
        db.update("UPDATE commerce_businesses SET default_transaction_currency=? WHERE id=?",code,business);
        db.update("INSERT INTO commerce_configuration_audit(id,business_id,actor_id,action,entity_id,detail) VALUES(?,?,?,'DEFAULT_CURRENCY_CHANGED',?,jsonb_build_object('previous',CAST(? AS text),'currency',CAST(? AS text),'rateToBase',CAST(? AS numeric)))",UUID.randomUUID(),business,actor,business,old,code,s.rateToBase());
        return s;
    }
    private ApiException stale(){return new ApiException(409,"CURRENCY_CHANGED","Currency or exchange rate changed. Refresh and review the converted prices again.");}
    private BigDecimal base(BigDecimal value,BigDecimal rate){if(value==null)return null;if(value.signum()<0)throw new ApiException(400,"INVALID_PRICE","Price cannot be negative.");return value.multiply(rate).setScale(4,RoundingMode.HALF_UP);}
    private PackageSetting pack(PackageSetting p,BigDecimal rate){return p==null?null:new PackageSetting(p.name(),p.factor(),base(p.retailPackagePrice(),rate),base(p.wholesalePackagePrice(),rate),base(p.purchasePackagePrice(),rate));}
    public Map<String,Object> save(UUID actor,UUID business,UUID id,String currency,BigDecimal expectedRate,NewProduct p){
        access.require(actor,business,"CORE",true);lock(business);var s=settings(actor,business);
        if(!s.currency().equals(currency)||expectedRate==null||s.rateToBase().compareTo(expectedRate)!=0)throw stale();
        var r=s.rateToBase();var u=p.product();
        var update=new CommerceCatalogService.ProductUpdate(u.name(),u.description(),u.categoryId(),u.barcode(),base(u.purchaseCost(),r),base(u.retailPrice(),r),base(u.wholesalePrice(),r),base(u.minimumPrice(),r),u.taxRate(),u.taxInclusive(),u.purchaseTaxRecoverable(),u.lowStockQuantity(),u.active(),u.discountEnabled(),u.cashierDiscountLimit(),u.returnWindowDays(),u.fractionalUnits());
        var packages=p.packageSettings()==null?null:p.packageSettings().stream().map(v->pack(v,r)).toList();
        var b=p.branchPrice();var branch=b==null?null:new BranchPrice(b.branchId(),base(b.price(),r),b.effectiveAt());
        if(id==null)return catalog.createConfigured(actor,business,new NewProduct(p.sku(),p.baseUnit(),update,pack(p.packageSetting(),r),branch,packages));
        catalog.configureProduct(actor,business,id,update,pack(p.packageSetting(),r),packages,branch);return Map.of("updated",true);
    }
}
