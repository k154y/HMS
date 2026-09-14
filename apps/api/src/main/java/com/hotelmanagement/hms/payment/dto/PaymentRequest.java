package com.hotelmanagement.hms.payment.dto;
import com.hotelmanagement.hms.payment.model.PaymentMethod; import jakarta.validation.constraints.*; import java.math.*; import java.util.*;
public record PaymentRequest(@NotNull UUID folioId,@NotNull PaymentMethod method,@Pattern(regexp="[A-Z]{3}") String currency,@NotNull @DecimalMin("0.0001") @Digits(integer=15,fraction=4) BigDecimal amount,@NotNull @DecimalMin("0.00000001") @Digits(integer=11,fraction=8) BigDecimal fxRate,@Size(max=255) String externalReference,@NotBlank @Size(max=128) String idempotencyKey){}
