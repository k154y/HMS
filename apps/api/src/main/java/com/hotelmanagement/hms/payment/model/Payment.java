package com.hotelmanagement.hms.payment.model;
import jakarta.persistence.*; import java.math.*; import java.time.*; import java.util.*;
@Entity @Table(name="payments")
public class Payment extends com.hotelmanagement.hms.shared.model.BranchEntity {
 @Column(name="folio_id") UUID folioId; @Column(name="cashier_user_id") UUID cashierUserId; @Enumerated(EnumType.STRING) PaymentMethod method;
 String currency; @Column(name="original_amount",precision=19,scale=4) BigDecimal originalAmount; @Column(name="fx_rate",precision=19,scale=8) BigDecimal fxRate; @Column(name="base_amount",precision=19,scale=4) BigDecimal baseAmount;
 @Column(name="external_reference") String externalReference; @Enumerated(EnumType.STRING) PaymentStatus status; @Column(name="idempotency_key") String idempotencyKey; @Column(name="actor_id") UUID actorId;
 protected Payment(){} public static Payment create(UUID h,UUID b,UUID folio,UUID cashier,PaymentMethod m,String currency,BigDecimal original,BigDecimal fx,BigDecimal base,String external,String key,UUID actor){var p=new Payment();p.hotelId=h;p.branchId=b;p.folioId=folio;p.cashierUserId=cashier;p.method=m;p.currency=currency;p.originalAmount=original;p.fxRate=fx;p.baseAmount=base;p.externalReference=external;p.idempotencyKey=key;p.actorId=actor;p.status=PaymentStatus.POSTED;return p;}
 public UUID getFolioId(){return folioId;} public PaymentMethod getMethod(){return method;} public String getCurrency(){return currency;} public BigDecimal getOriginalAmount(){return originalAmount;} public BigDecimal getFxRate(){return fxRate;} public BigDecimal getBaseAmount(){return baseAmount;} public PaymentStatus getStatus(){return status;}
 public void voidPayment(){if(status!=PaymentStatus.POSTED)throw new IllegalStateException("Payment is not posted.");status=PaymentStatus.VOIDED;} public void refund(){if(status!=PaymentStatus.POSTED)throw new IllegalStateException("Payment is not posted.");status=PaymentStatus.REFUNDED;}
}
