package com.hotelmanagement.hms.product.dto;
import com.hotelmanagement.hms.product.model.Destination;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record ProductRequest(@NotBlank @Size(max=100) String sku,@NotBlank @Size(max=200) String name,@NotBlank @Size(max=100) String category,
 @NotBlank @Size(max=30) String purchaseUnit,@NotBlank @Size(max=30) String sellingUnit,@NotBlank @Size(max=30) String stockUnit,
 @NotNull @DecimalMin("0.0001") @Digits(integer=15,fraction=4) BigDecimal purchaseFactor,
 @NotNull @DecimalMin("0.0001") @Digits(integer=15,fraction=4) BigDecimal sellingFactor,
 @NotNull @DecimalMin("0") @Digits(integer=15,fraction=4) BigDecimal purchasePrice,
 @NotNull @DecimalMin("0") @Digits(integer=15,fraction=4) BigDecimal sellingPrice,
 @NotNull @DecimalMin("0") @DecimalMax("1") @Digits(integer=1,fraction=4) BigDecimal taxRate,
 boolean stockTracked,boolean sellable,boolean purchasable,boolean active,
 @NotNull @DecimalMin("0") @Digits(integer=15,fraction=4) BigDecimal reorderLevel,@NotNull Destination destination) {}
