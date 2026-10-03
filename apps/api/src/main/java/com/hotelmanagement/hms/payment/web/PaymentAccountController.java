package com.hotelmanagement.hms.payment.web;
import com.hotelmanagement.hms.payment.service.PaymentAccountService;
import com.hotelmanagement.hms.payment.service.PaymentAccountService.Account;
import com.hotelmanagement.hms.platform.currency.service.HotelCurrencyService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/hotels/{hotel}")
public class PaymentAccountController {
    private final PaymentAccountService accounts;private final HotelCurrencyService currencies;
    public PaymentAccountController(PaymentAccountService accounts,HotelCurrencyService currencies){this.accounts=accounts;this.currencies=currencies;}
    @GetMapping("/payment-accounts") public List<PaymentAccountService.AccountResponse> accounts(@PathVariable UUID hotel){return accounts.list(hotel);}
    @PostMapping("/payment-accounts") public PaymentAccountService.AccountResponse create(@PathVariable UUID hotel,@RequestBody Account request){return PaymentAccountService.response(accounts.save(hotel,null,request));}
    @PutMapping("/payment-accounts/{id}") public PaymentAccountService.AccountResponse update(@PathVariable UUID hotel,@PathVariable UUID id,@RequestBody Account request){return PaymentAccountService.response(accounts.save(hotel,id,request));}
    @GetMapping("/currencies/base") public Object base(@PathVariable UUID hotel){currencies.list(hotel);return Map.of("currency",currencies.base(hotel));}
    @GetMapping("/currencies") public Object currencies(@PathVariable UUID hotel){return currencies.list(hotel);}
    @PutMapping("/currencies/{code}") public void currency(@PathVariable UUID hotel,@PathVariable String code,@RequestBody HotelCurrencyService.CurrencyOption request){currencies.save(hotel,code,request.name(),request.enabled());}
}
