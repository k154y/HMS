package com.hotelmanagement.hms.report.web;

import com.hotelmanagement.hms.shared.service.OperationScope;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.FINANCIAL_REPORT_VIEW;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@RestController
@RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/reports")
public class AccountingReportController {
 private final JdbcTemplate db; private final OperationScope scope;
 public AccountingReportController(JdbcTemplate db,OperationScope scope){this.db=db;this.scope=scope;}
 private BigDecimal sum(String sql,Object... args){return db.queryForObject(sql,BigDecimal.class,args);}
 @GetMapping("/accounting") @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public Map<String,Object> accounting(@PathVariable UUID hotel,@PathVariable UUID branch,@RequestParam LocalDate from,@RequestParam LocalDate to){
  scope.branch(hotel,branch,FINANCIAL_REPORT_VIEW);
  if(to.isBefore(from)||to.isAfter(from.plusYears(5)))throw new IllegalArgumentException("Invalid report date range (maximum five years).");
  var info=db.queryForMap("select currency_code as base_currency,timezone from hotels where id=?",hotel);
  var zone=ZoneId.of((String)info.get("timezone"));
  var start=Timestamp.from(from.atStartOfDay(zone).toInstant());var end=Timestamp.from(to.plusDays(1).atStartOfDay(zone).toInstant());
  var result=new LinkedHashMap<String,Object>();
  result.put("period",Map.of("from",from,"to",to,"currency",info.get("base_currency"),"timezone",zone.getId(),"balancesAt",Instant.now()));
  String ledger=" from folio_entries where hotel_id=? and branch_id=? and created_at>=? and created_at<?";
  var revenue=sum("select coalesce(sum(amount),0)"+ledger+" and kind in ('ACCOMMODATION','ORDER','CHARGE','REVERSAL')",hotel,branch,start,end);
  var received=sum("select -coalesce(sum(amount),0)"+ledger+" and kind='PAYMENT'",hotel,branch,start,end);
  var refunds=sum("select coalesce(sum(amount),0)"+ledger+" and kind='REFUND'",hotel,branch,start,end);
  var vendorPaid=sum("select coalesce(sum(amount),0) from vendor_payments where hotel_id=? and branch_id=? and created_at>=? and created_at<?",hotel,branch,start,end);
  var expensePaid=sum("select coalesce(sum(amount),0) from expenses where hotel_id=? and branch_id=? and created_at>=? and created_at<?",hotel,branch,start,end);
  var expenseRecognized=sum("select coalesce(sum(amount),0) from expenses where hotel_id=? and branch_id=? and expense_date>=? and expense_date<=?",hotel,branch,from,to);
  result.put("summary",Map.of("billedRevenue",revenue,"receipts",received,"refunds",refunds,"supplierPayments",vendorPaid,"expensePayments",expensePaid,"operatingExpenses",expenseRecognized,"netCashMovement",received.subtract(refunds).subtract(vendorPaid).subtract(expensePaid)));
  result.put("dailySales",db.queryForList("select cast(created_at at time zone ? as date) report_date,coalesce(sum(amount),0) amount from folio_entries where hotel_id=? and branch_id=? and created_at>=? and created_at<? and kind in ('ACCOMMODATION','ORDER','CHARGE','REVERSAL') group by report_date order by report_date",zone.getId(),hotel,branch,start,end));
  result.put("paymentMethods",db.queryForList("select p.method,coalesce(-sum(e.amount) filter(where e.kind='PAYMENT'),0) receipts,coalesce(sum(e.amount) filter(where e.kind='REFUND'),0) refunds,-sum(e.amount) net from folio_entries e join payments p on p.id=e.source_id where e.hotel_id=? and e.branch_id=? and e.created_at>=? and e.created_at<? and e.kind in ('PAYMENT','REFUND') group by p.method order by p.method",hotel,branch,start,end));
  result.put("expenses",db.queryForList("select id,expense_date,category,description,method,amount from expenses where hotel_id=? and branch_id=? and expense_date>=? and expense_date<=? order by expense_date,created_at,id",hotel,branch,from,to));
  result.put("purchases",db.queryForList("select p.id,p.created_at,p.reference,v.name vendor,p.status,p.total,coalesce((select sum(amount) from vendor_payments vp where vp.purchase_order_id=p.id),0) paid from purchase_orders p join vendors v on v.id=p.vendor_id where p.hotel_id=? and p.branch_id=? and p.created_at>=? and p.created_at<? order by p.created_at,p.id",hotel,branch,start,end));
  result.put("supplierPayments",db.queryForList("select vp.id,vp.created_at,v.name vendor,p.reference,vp.method,vp.amount from vendor_payments vp join purchase_orders p on p.id=vp.purchase_order_id join vendors v on v.id=p.vendor_id where vp.hotel_id=? and vp.branch_id=? and vp.created_at>=? and vp.created_at<? order by vp.created_at,vp.id",hotel,branch,start,end));
  var debtors=db.queryForList("select f.id,c.name customer,c.kind customer_type,f.status,f.currency,coalesce(sum(e.amount),0) balance from folios f join customers c on c.id=f.customer_id left join folio_entries e on e.folio_id=f.id where f.hotel_id=? and f.branch_id=? group by f.id,c.name,c.kind having coalesce(sum(e.amount),0)>0 order by c.name,f.id",hotel,branch);
  result.put("customerBalances",debtors);
  result.put("supplierBalances",db.queryForList("select p.id,p.reference,v.name vendor,p.total,coalesce(sum(vp.amount),0) paid,p.total-coalesce(sum(vp.amount),0) balance from purchase_orders p join vendors v on v.id=p.vendor_id left join vendor_payments vp on vp.purchase_order_id=p.id where p.hotel_id=? and p.branch_id=? and p.status in ('APPROVED','RECEIVED') group by p.id,v.name having p.total>coalesce(sum(vp.amount),0) order by v.name,p.id",hotel,branch));
  result.put("billActivity",db.queryForList("select o.id,o.created_at,c.name customer,o.status,o.total,f.status folio_status from orders o join customers c on c.id=o.customer_id join folios f on f.id=o.folio_id where o.hotel_id=? and o.branch_id=? and o.created_at>=? and o.created_at<? order by o.created_at,o.id",hotel,branch,start,end));
  result.put("shifts",db.queryForList("select s.id,u.full_name cashier,s.opened_at,s.closed_at,s.status,s.opening_float,s.expected_amount,s.counted_amount,s.difference from cashier_shifts s join users u on u.id=s.cashier_user_id where s.hotel_id=? and s.branch_id=? and s.opened_at>=? and s.opened_at<? order by s.opened_at,s.id",hotel,branch,start,end));
  return result;
 }
}

