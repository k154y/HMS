package com.hotelmanagement.hms.payment.service;

import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.payment.model.PaymentMethod;
import com.hotelmanagement.hms.platform.currency.service.HotelCurrencyService;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;

@Service @Transactional
public class PaymentAccountService {
    private final JdbcTemplate db; private final OperationScope scope; private final AuditService audit; private final HotelCurrencyService currencies;
    public PaymentAccountService(JdbcTemplate db,OperationScope scope,AuditService audit,HotelCurrencyService currencies) {this.db=db;this.scope=scope;this.audit=audit;this.currencies=currencies;}
    public record Account(UUID id,String name,PaymentMethod method,String currency,String identifierType,String identifier,boolean active) {
        @Override public String toString() { return "PaymentAccount[REDACTED]"; }
    }
    public record AccountResponse(UUID id,String name,PaymentMethod method,String currency,String identifierType,String maskedIdentifier,boolean active) {}
    public static AccountResponse response(Account account) {
        return new AccountResponse(account.id(),account.name(),account.method(),account.currency(),account.identifierType(),PaymentAccountMasking.mask(account.identifier()),account.active());
    }
    private Account row(java.sql.ResultSet r,int n)throws java.sql.SQLException {
        return new Account(r.getObject("id",UUID.class),r.getString("name"),PaymentMethod.valueOf(r.getString("method")),r.getString("currency"),r.getString("identifier_type"),r.getString("identifier"),r.getBoolean("active"));
    }
    @Transactional(readOnly=true) public List<AccountResponse> list(UUID hotel) {scope.hotel(hotel,BRANCH_VIEW);return db.query("select * from payment_accounts where hotel_id=? order by name",this::row,hotel).stream().map(PaymentAccountService::response).toList();}
    public Account save(UUID hotel,UUID id,Account request) {
        UUID actor=scope.hotel(hotel,HOTEL_SETTINGS_MANAGE);
        if(request.name()==null||request.name().isBlank()||request.name().length()>100)
            throw new IllegalArgumentException("Account name and payment type are required.");
        if(id!=null) {
            var existing=db.query("select * from payment_accounts where id=? and hotel_id=? for update",this::row,id,hotel);
            if(existing.isEmpty())throw ApiException.notFound();
            var old=existing.getFirst();
            // Older clients may send identity fields. Reject changes; never overwrite them.
            if((request.method()!=null && old.method()!=request.method())
                    || (request.currency()!=null && !old.currency().equals(request.currency()))
                    || (request.identifierType()!=null && !old.identifierType().equals(request.identifierType()))
                    || (request.identifier()!=null && !Objects.equals(old.identifier(),request.identifier())))
                throw new IllegalArgumentException("Create a new account to change its type, currency or identifier.");
            db.update("update payment_accounts set name=?,active=? where id=? and hotel_id=?",request.name().trim(),request.active(),id,hotel);
            audit.record(hotel,null,actor,"PAYMENT_ACCOUNT_SAVED","PAYMENT_ACCOUNT",id);
            return new Account(id,request.name().trim(),old.method(),old.currency(),old.identifierType(),old.identifier(),request.active());
        }
        if(request.method()==null||request.method()==PaymentMethod.CREDIT)throw new IllegalArgumentException("Payment type is required.");
        String currency=currencies.requireEnabled(hotel,request.currency());
        String type=request.identifierType();String identifier=request.identifier()==null?null:request.identifier().trim();
        if(type==null||!Set.of("NONE","ACCOUNT_NUMBER","PHONE_NUMBER","MERCHANT_CODE").contains(type))throw new IllegalArgumentException("Invalid account identifier type.");
        if(request.method()==PaymentMethod.BANK_TRANSFER&&!type.equals("ACCOUNT_NUMBER"))throw new IllegalArgumentException("Bank accounts require an account number.");
        if(request.method()==PaymentMethod.MOBILE_MONEY&&!Set.of("PHONE_NUMBER","MERCHANT_CODE").contains(type))throw new IllegalArgumentException("Mobile money requires a phone number or merchant code.");
        if(!type.equals("NONE")&&(identifier==null||identifier.isBlank()||identifier.length()>100))throw new IllegalArgumentException("Account identifier is required (maximum 100 characters).");
        if(type.equals("NONE"))identifier=null;
        id=UUID.randomUUID();
        // Hotels created after migration also need their base currency registered.
        db.update("insert into hotel_currencies(hotel_id,code,name) values(?,?,?) on conflict do nothing",hotel,currency,currency);
        db.update("insert into payment_accounts(id,hotel_id,name,method,currency,identifier_type,identifier,active) values(?,?,?,?,?,?,?,?)",id,hotel,request.name().trim(),request.method().name(),currency,type,identifier,request.active());
        audit.record(hotel,null,actor,"PAYMENT_ACCOUNT_SAVED","PAYMENT_ACCOUNT",id);
        return new Account(id,request.name().trim(),request.method(),currency,type,identifier,request.active());
    }
    public Account resolve(UUID hotel,UUID id,PaymentMethod method,String currency) {
        if(id==null)return null; // Existing clients and historical approvals remain valid.
        var result=db.query("select * from payment_accounts where id=? and hotel_id=? for share",this::row,id,hotel);
        if(result.isEmpty())throw ApiException.notFound();var account=result.getFirst();
        if(!account.active()||account.method()!=method||!account.currency().equals(currency))throw new IllegalArgumentException("Select an active account matching the payment type and currency.");
        return account;
    }
}
