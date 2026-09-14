package com.hotelmanagement.hms.order.model;
import jakarta.persistence.*; import java.math.*; import java.util.*;
@Entity @Table(name="orders") public class Order extends com.hotelmanagement.hms.shared.model.BranchEntity {
 @Column(name="customer_id") UUID customerId; @Column(name="folio_id") UUID folioId; @Column(name="server_user_id") UUID serverUserId; String destination; @Enumerated(EnumType.STRING) OrderStatus status; BigDecimal subtotal,tax,discount,total;
 protected Order(){} public static Order create(UUID h,UUID b,UUID c,UUID f,UUID actor,String d){var e=new Order();e.hotelId=h;e.branchId=b;e.customerId=c;e.folioId=f;e.serverUserId=actor;e.destination=d;e.status=OrderStatus.DRAFT;e.total=BigDecimal.ZERO;e.subtotal=BigDecimal.ZERO;e.tax=BigDecimal.ZERO;e.discount=BigDecimal.ZERO;return e;}
 public UUID getCustomerId(){return customerId;} public UUID getFolioId(){return folioId;} public String getDestination(){return destination;} public OrderStatus getStatus(){return status;} public BigDecimal getTotal(){return total;}
 public void totals(BigDecimal subtotal,BigDecimal tax){this.subtotal=subtotal;this.tax=tax;this.total=subtotal.add(tax).subtract(discount==null?BigDecimal.ZERO:discount);}
 public void send(){if(status!=OrderStatus.DRAFT)throw new IllegalStateException("Order is not draft.");status=OrderStatus.SENT;} public void voidOrder(){if(status==OrderStatus.VOIDED)throw new IllegalStateException("Order already voided.");status=OrderStatus.VOIDED;}
 public void advance(OrderStatus next){
  if (!(status==OrderStatus.SENT && next==OrderStatus.PREPARING || status==OrderStatus.PREPARING && next==OrderStatus.READY || status==OrderStatus.READY && next==OrderStatus.SERVED)) throw new IllegalStateException("Invalid order transition.");
  status=next;
 }
}
