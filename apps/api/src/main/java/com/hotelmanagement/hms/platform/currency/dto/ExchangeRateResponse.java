package com.hotelmanagement.hms.platform.currency.dto;

import com.hotelmanagement.hms.platform.currency.model.HotelExchangeRate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExchangeRateResponse(
        UUID id,
        UUID hotelId,
        String baseCurrencyCode,
        String currencyCode,
        BigDecimal rateToBase,
        boolean enabled,
        OffsetDateTime effectiveFrom,
        OffsetDateTime createdAt
) {

    public static ExchangeRateResponse from(
            HotelExchangeRate exchangeRate) {

        return new ExchangeRateResponse(
                exchangeRate.getId(),
                exchangeRate.getHotel().getId(),
                exchangeRate.getBaseCurrencyCode(),
                exchangeRate.getCurrencyCode(),
                exchangeRate.getRateToBase(),
                exchangeRate.isEnabled(),
                exchangeRate.getEffectiveFrom(),
                exchangeRate.getCreatedAt()
        );
    }
}