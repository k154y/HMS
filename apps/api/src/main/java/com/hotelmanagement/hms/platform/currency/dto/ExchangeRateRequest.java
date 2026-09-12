package com.hotelmanagement.hms.platform.currency.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExchangeRateRequest(

        @NotBlank
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Currency code must contain exactly three letters.")
        String currencyCode,

        @NotNull
        @DecimalMin(
                value = "0.00000001",
                message = "Exchange rate must be greater than zero.")
        @Digits(
                integer = 11,
                fraction = 8,
                message = "Exchange rate supports up to 11 integer digits and 8 decimal places.")
        BigDecimal rateToBase,

        OffsetDateTime effectiveFrom
) {
}