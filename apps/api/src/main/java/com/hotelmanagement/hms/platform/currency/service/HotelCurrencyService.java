package com.hotelmanagement.hms.platform.currency.service;

import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.OffsetDateTime;
import java.util.*;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;

@Service
@Transactional
public class HotelCurrencyService {
    private final JdbcTemplate db;
    private final OperationScope scope;
    private final AuditService audit;
    private final ExchangeRateService rates;
    public HotelCurrencyService(JdbcTemplate db,OperationScope scope,AuditService audit,ExchangeRateService rates) {
        this.db=db;this.scope=scope;this.audit=audit;this.rates=rates;
    }
    public record CurrencyOption(String code,String name,boolean enabled) {}
    public record Quote(String currency,BigDecimal fxRate,BigDecimal baseAmount) {}
    public String base(UUID hotel) {
        var rows=db.queryForList("select currency_code from hotels where id=?",String.class,hotel);
        if(rows.isEmpty())throw ApiException.notFound();return rows.getFirst();
    }
    @Transactional(readOnly=true)
    public List<CurrencyOption> list(UUID hotel) {
        scope.hotel(hotel,BRANCH_VIEW);
        var result=new ArrayList<>(db.query("select code,name,enabled from hotel_currencies where hotel_id=? order by code",
                (rs,n)->new CurrencyOption(rs.getString(1),rs.getString(2),rs.getBoolean(3)),hotel));
        String base=base(hotel);
        if(result.stream().noneMatch(c->c.code().equals(base)))result.addFirst(new CurrencyOption(base,base,true));
        return result;
    }
    public void save(UUID hotel,String code,String name,boolean enabled) {
        UUID actor=scope.hotel(hotel,EXCHANGE_RATE_MANAGE);code=normalize(code);
        if(name==null||name.isBlank()||name.length()>100)throw new IllegalArgumentException("Currency name is required (maximum 100 characters).");
        if(code.equals(base(hotel))&&!enabled)throw new IllegalArgumentException("The accounting currency must remain enabled.");
        db.update("insert into hotel_currencies(hotel_id,code,name,enabled) values(?,?,?,?) on conflict(hotel_id,code) do update set name=excluded.name,enabled=excluded.enabled",hotel,code,name.trim(),enabled);
        audit.record(hotel,null,actor,"CURRENCY_UPDATED","HOTEL",hotel);
    }
    public String requireEnabled(UUID hotel,String code) {
        String normalized=code==null||code.isBlank()?base(hotel):normalize(code);
        if(normalized.equals(base(hotel))) return normalized;
        if(!Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from hotel_currencies where hotel_id=? and code=? and enabled)",Boolean.class,hotel,normalized)))
            throw new IllegalArgumentException("Currency is not enabled for this hotel.");
        return normalized;
    }
    @Transactional(readOnly=true)
    public BigDecimal preview(UUID hotel,String code,BigDecimal amount) {
        String currency=code==null?base(hotel):code;
        if(currency.equals(base(hotel)))return amount;
        var matches=db.queryForList("select r.rate_to_base from hotel_exchange_rates r join hotel_currencies c on c.hotel_id=r.hotel_id and c.code=r.currency_code where r.hotel_id=? and r.currency_code=? and c.enabled and r.effective_from<=current_timestamp order by r.effective_from desc limit 1",BigDecimal.class,hotel,currency);
        return matches.isEmpty()?null:amount.multiply(matches.getFirst()).setScale(4,RoundingMode.HALF_UP);
    }
    public Quote quote(UUID hotel,String code,BigDecimal amount) {
        String currency=requireEnabled(hotel,code);
        if(amount==null||amount.signum()<0||amount.scale()>4||amount.precision()-amount.scale()>15)throw new IllegalArgumentException("Invalid monetary amount.");
        BigDecimal rate=currency.equals(base(hotel))?BigDecimal.ONE:rates.getApplicableRate(hotel,currency,OffsetDateTime.now()).rateToBase();
        BigDecimal converted=amount.multiply(rate).setScale(4,RoundingMode.HALF_UP);
        if(converted.precision()-converted.scale()>15)throw new IllegalArgumentException("Converted amount is too large.");
        return new Quote(currency,rate,converted);
    }
    private String normalize(String code) {
        if(code==null)throw new IllegalArgumentException("Currency is required.");
        String normalized=code.trim().toUpperCase(Locale.ROOT);
        try { java.util.Currency.getInstance(normalized); } catch(IllegalArgumentException failure) { throw new IllegalArgumentException("Choose a valid three-letter currency code."); }
        return normalized;
    }
}
