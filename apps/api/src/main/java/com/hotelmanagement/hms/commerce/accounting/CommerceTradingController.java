package com.hotelmanagement.hms.commerce.accounting;

import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/commerce/businesses/{business}")
public class CommerceTradingController {
    private final CommerceTradingService trading;private final CommerceFinanceService finance;private final CommerceLedgerService ledger;private final AuthenticatedUserContext current;
    public CommerceTradingController(CommerceTradingService trading,CommerceFinanceService finance,CommerceLedgerService ledger,AuthenticatedUserContext current){this.trading=trading;this.finance=finance;this.ledger=ledger;this.current=current;}
    private UUID actor(){return current.requireCurrentUserId();}
    public record Request(@jakarta.validation.constraints.NotNull UUID requestId) {}
    /** Keep decimal amounts exact across the JavaScript JSON boundary. */
    private Object wire(Object value){
        if(value instanceof java.math.BigDecimal decimal)return decimal.toPlainString();
        if(value instanceof Map<?,?> map){var result=new LinkedHashMap<String,Object>();map.forEach((key,item)->result.put(key.toString(),wire(item)));return result;}
        if(value instanceof List<?> list)return list.stream().map(this::wire).toList();
        return value;
    }
    @GetMapping("/accounting") public Object state(@PathVariable UUID business){return wire(finance.state(actor(),business));}
    @PostMapping("/accounting/activate") public Object activate(@PathVariable UUID business,@jakarta.validation.Valid @RequestBody Request p){return wire(ledger.activate(actor(),business,p.requestId()));}
    @GetMapping("/parties") public Object parties(@PathVariable UUID business){return wire(trading.parties(actor(),business));}
    @PostMapping("/parties") public Object party(@PathVariable UUID business,@jakarta.validation.Valid @RequestBody CommerceTradingService.Party p){return wire(trading.party(actor(),business,p));}
    @PostMapping("/parties/{party}/update") public Object party(@PathVariable UUID business,@PathVariable UUID party,@jakarta.validation.Valid @RequestBody CommerceTradingService.PartyUpdate p){return wire(trading.updateParty(actor(),business,party,p));}
    @PostMapping("/documents/{document}/print") public Object print(@PathVariable UUID business,@PathVariable UUID document,@jakarta.validation.Valid @RequestBody Request p){return wire(trading.print(actor(),business,document,p.requestId()));}
    @GetMapping("/payment-accounts") public Object accounts(@PathVariable UUID business){return wire(trading.accounts(actor(),business));}
    @PostMapping("/payment-accounts") public Object account(@PathVariable UUID business,@jakarta.validation.Valid @RequestBody CommerceTradingService.Account p){return wire(trading.account(actor(),business,p));}
    @PostMapping("/payment-accounts/{account}/update") public Object updateAccount(@PathVariable UUID business,@PathVariable UUID account,@jakarta.validation.Valid @RequestBody CommerceTradingService.AccountUpdate p){return wire(trading.updateAccount(actor(),business,account,p));}
    @PostMapping("/documents") public Object invoice(@PathVariable UUID business,@jakarta.validation.Valid @RequestBody CommerceTradingService.Invoice p){return wire(trading.invoice(actor(),business,p));}
    @PostMapping("/documents/preview") public Object preview(@PathVariable UUID business,@jakarta.validation.Valid @RequestBody CommerceTradingService.Invoice p){return wire(trading.preview(actor(),business,p));}
    @GetMapping("/documents/{document}") public Object detail(@PathVariable UUID business,@PathVariable UUID document){return wire(trading.detail(actor(),business,document));}
    @GetMapping("/branches/{branch}/documents") public Object history(@PathVariable UUID business,@PathVariable UUID branch){return wire(trading.history(actor(),business,branch));}
    @PostMapping("/receiving") public Object receive(@PathVariable UUID business,@jakarta.validation.Valid @RequestBody CommerceTradingService.Receive p){return wire(trading.receive(actor(),business,p));}
    @PostMapping("/settlements") public Object settle(@PathVariable UUID business,@jakarta.validation.Valid @RequestBody CommerceTradingService.Settlement p){return wire(trading.settle(actor(),business,p));}
    @PostMapping("/returns") public Object returns(@PathVariable UUID business,@jakarta.validation.Valid @RequestBody CommerceTradingService.Return p){return wire(trading.returnGoods(actor(),business,p));}
    @PostMapping("/shifts") public Object open(@PathVariable UUID business,@jakarta.validation.Valid @RequestBody CommerceFinanceService.Shift p){return wire(finance.open(actor(),business,p));}
    @GetMapping("/branches/{branch}/shifts") public Object shifts(@PathVariable UUID business,@PathVariable UUID branch){return wire(finance.shifts(actor(),business,branch));}
    @PostMapping("/shifts/{shift}/close") public Object close(@PathVariable UUID business,@PathVariable UUID shift,@jakarta.validation.Valid @RequestBody CommerceFinanceService.Close p){return wire(finance.close(actor(),business,shift,p));}
    @PostMapping("/shifts/{shift}/approve") public Object approve(@PathVariable UUID business,@PathVariable UUID shift,@jakarta.validation.Valid @RequestBody Request p){return wire(finance.approve(actor(),business,shift,p.requestId()));}
    @PostMapping("/shifts/{shift}/cash-moves") public Object move(@PathVariable UUID business,@PathVariable UUID shift,@jakarta.validation.Valid @RequestBody CommerceFinanceService.CashMove p){return wire(finance.cashMove(actor(),business,shift,p));}
    @PostMapping("/expenses") public Object expense(@PathVariable UUID business,@jakarta.validation.Valid @RequestBody CommerceFinanceService.Expense p){return wire(finance.expense(actor(),business,p));}
    @GetMapping("/reports") public Object reports(@PathVariable UUID business,@RequestParam(required=false) UUID branch,@RequestParam LocalDate from,@RequestParam LocalDate to){return wire(finance.reports(actor(),business,branch,from,to));}
}

