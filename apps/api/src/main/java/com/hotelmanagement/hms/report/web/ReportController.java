package com.hotelmanagement.hms.report.web;
import com.hotelmanagement.hms.shared.service.OperationScope;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;import java.time.*;
@RestController @RequestMapping("/api/v1/hotels/{hotel}/branches/{branch}/reports")
public class ReportController {
 private final JdbcTemplate db;private final OperationScope scope;
 public ReportController(JdbcTemplate d,OperationScope s){db=d;scope=s;}
 private void range(LocalDate from,LocalDate to){if(from==null||to==null||to.isBefore(from)||to.isAfter(from.plusYears(5)))throw new IllegalArgumentException("Invalid report date range (maximum five years).");}
 @GetMapping("/sales") public Map<String,Object> sales(@PathVariable UUID hotel,@PathVariable UUID branch,@RequestParam LocalDate from,@RequestParam LocalDate to){
  scope.branch(hotel,branch,REPORT_VIEW);range(from,to);
  var result=new LinkedHashMap<String,Object>();
  result.put("summary",db.queryForMap("select coalesce(sum(total),0) total,coalesce(sum(subtotal),0) subtotal,coalesce(sum(tax),0) tax,count(*) orders from orders where hotel_id=? and branch_id=? and created_at>=? and created_at<? and status not in ('DRAFT','VOIDED')",hotel,branch,from,to.plusDays(1)));
  result.put("daily",db.queryForList("select cast(created_at as date) as report_date,sum(total) total,count(*) orders from orders where hotel_id=? and branch_id=? and created_at>=? and created_at<? and status not in ('DRAFT','VOIDED') group by cast(created_at as date) order by report_date",hotel,branch,from,to.plusDays(1)));
  result.put("creditSales",db.queryForList("select o.id,o.created_at,o.total,c.name customer from orders o join folios f on f.id=o.folio_id join customers c on c.id=o.customer_id where o.hotel_id=? and o.branch_id=? and o.created_at>=? and o.created_at<? and o.status not in ('DRAFT','VOIDED') and f.status='CREDIT' order by o.created_at desc",hotel,branch,from,to.plusDays(1)));
  return result;
 }
 @GetMapping("/financial") public Map<String,Object> financial(@PathVariable UUID hotel,@PathVariable UUID branch,@RequestParam LocalDate from,@RequestParam LocalDate to){
  scope.branch(hotel,branch,FINANCIAL_REPORT_VIEW);range(from,to);var result=new LinkedHashMap<String,Object>();
  result.put("paymentsByMethod",db.queryForList("select method,status,sum(base_amount) amount,count(*) transactions from payments where hotel_id=? and branch_id=? and created_at>=? and created_at<? group by method,status order by method,status",hotel,branch,from,to.plusDays(1)));
  result.put("ledgerActivity",db.queryForList("select kind,sum(amount) amount from folio_entries where hotel_id=? and branch_id=? and created_at>=? and created_at<? group by kind order by kind",hotel,branch,from,to.plusDays(1)));
  result.put("vendorPayments",db.queryForList("select method,sum(amount) amount from vendor_payments where hotel_id=? and branch_id=? and created_at>=? and created_at<? group by method",hotel,branch,from,to.plusDays(1)));
  result.put("expenses",db.queryForList("select category,method,sum(amount) amount from expenses where hotel_id=? and branch_id=? and expense_date>=? and expense_date<=? group by category,method",hotel,branch,from,to));
  result.put("outstandingPurchases",db.queryForList("select p.id,p.reference,v.name vendor,p.total,coalesce(sum(vp.amount),0) paid,p.total-coalesce(sum(vp.amount),0) balance from purchase_orders p join vendors v on v.id=p.vendor_id left join vendor_payments vp on vp.purchase_order_id=p.id where p.hotel_id=? and p.branch_id=? and p.status in ('APPROVED','RECEIVED') group by p.id,v.name having p.total>coalesce(sum(vp.amount),0)",hotel,branch));
  return result;
 }
 @GetMapping("/transactions") public List<Map<String,Object>> transactions(@PathVariable UUID hotel,@PathVariable UUID branch,@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam com.hotelmanagement.hms.payment.model.PaymentMethod method){scope.branch(hotel,branch,PAYMENT_VIEW);range(from,to);return db.queryForList("select p.*,c.name customer from payments p join folios f on f.id=p.folio_id join customers c on c.id=f.customer_id where p.hotel_id=? and p.branch_id=? and p.method=? and p.created_at>=? and p.created_at<? order by p.created_at desc limit 1000",hotel,branch,method.name(),from,to.plusDays(1));}
}


