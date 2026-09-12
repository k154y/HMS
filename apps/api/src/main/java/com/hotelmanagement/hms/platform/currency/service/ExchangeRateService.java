package com.hotelmanagement.hms.platform.currency.service;

import com.hotelmanagement.hms.platform.currency.dto.ExchangeRateRequest;
import com.hotelmanagement.hms.platform.currency.dto.ExchangeRateResponse;
import com.hotelmanagement.hms.platform.currency.model.HotelExchangeRate;
import com.hotelmanagement.hms.platform.currency.repository.HotelExchangeRateRepository;
import com.hotelmanagement.hms.platform.model.Hotel;
import com.hotelmanagement.hms.platform.repository.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

@Service
public class ExchangeRateService {

    private static final int EXCHANGE_RATE_SCALE = 8;
    private static final int MONEY_SCALE = 4;

    private final HotelRepository hotelRepository;
    private final HotelExchangeRateRepository exchangeRateRepository;

    public ExchangeRateService(
            HotelRepository hotelRepository,
            HotelExchangeRateRepository exchangeRateRepository) {

        this.hotelRepository = hotelRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    /**
     * Records a new hotel exchange rate.
     *
     * Existing rates are never modified because historical payments
     * must remain traceable to the rate that existed at the time.
     */
    @Transactional
    public ExchangeRateResponse setExchangeRate(
            UUID hotelId,
            ExchangeRateRequest request) {

        Hotel hotel = getHotel(hotelId);

        String baseCurrency =
                normalizeCurrencyCode(
                        hotel.getCurrencyCode());

        String foreignCurrency =
                normalizeCurrencyCode(
                        request.currencyCode());

        if (baseCurrency.equals(foreignCurrency)) {
            throw new IllegalArgumentException(
                    "An exchange rate is not required for "
                            + "the hotel's base currency.");
        }

        BigDecimal rate =
                normalizeRate(request.rateToBase());

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        OffsetDateTime effectiveFrom =
                request.effectiveFrom() == null
                        ? now
                        : request.effectiveFrom();

        HotelExchangeRate exchangeRate =
                HotelExchangeRate.create(
                        hotel,
                        baseCurrency,
                        foreignCurrency,
                        rate,
                        true,
                        effectiveFrom,
                        now
                );

        HotelExchangeRate savedRate =
                exchangeRateRepository.saveAndFlush(
                        exchangeRate);

        return ExchangeRateResponse.from(savedRate);
    }

    /**
     * Returns the historical exchange rate that was applicable
     * at the requested point in time.
     */
    @Transactional(readOnly = true)
    public ExchangeRateResponse getApplicableRate(
            UUID hotelId,
            String currencyCode,
            OffsetDateTime effectiveAt) {

        Hotel hotel = getHotel(hotelId);

        String baseCurrency =
                normalizeCurrencyCode(
                        hotel.getCurrencyCode());

        String requestedCurrency =
                normalizeCurrencyCode(currencyCode);

        if (baseCurrency.equals(requestedCurrency)) {
            throw new IllegalArgumentException(
                    "The requested currency is already "
                            + "the hotel's base currency.");
        }

        HotelExchangeRate exchangeRate =
                findApplicableRate(
                        hotelId,
                        requestedCurrency,
                        effectiveAt
                );

        return ExchangeRateResponse.from(exchangeRate);
    }

    /**
     * Converts an amount into the hotel's base/accounting currency.
     *
     * Example:
     *
     * Hotel base currency = RWF
     * Payment currency = USD
     * Payment amount = 200
     * Rate = 1450
     *
     * Result = 290000 RWF
     */
    @Transactional(readOnly = true)
    public BigDecimal convertToBaseCurrency(
            UUID hotelId,
            String paymentCurrencyCode,
            BigDecimal paymentAmount,
            OffsetDateTime effectiveAt) {

        validateAmount(paymentAmount);

        Hotel hotel = getHotel(hotelId);

        String baseCurrency =
                normalizeCurrencyCode(
                        hotel.getCurrencyCode());

        String paymentCurrency =
                normalizeCurrencyCode(
                        paymentCurrencyCode);

        /*
         * If the customer pays directly in the hotel's base currency,
         * no foreign-exchange record is necessary.
         */
        if (baseCurrency.equals(paymentCurrency)) {
            return paymentAmount.setScale(
                    MONEY_SCALE,
                    RoundingMode.HALF_UP
            );
        }

        HotelExchangeRate exchangeRate =
                findApplicableRate(
                        hotelId,
                        paymentCurrency,
                        effectiveAt
                );

        return paymentAmount
                .multiply(exchangeRate.getRateToBase())
                .setScale(
                        MONEY_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    private HotelExchangeRate findApplicableRate(
            UUID hotelId,
            String currencyCode,
            OffsetDateTime effectiveAt) {

        OffsetDateTime lookupTime =
                effectiveAt == null
                        ? OffsetDateTime.now(ZoneOffset.UTC)
                        : effectiveAt;

        HotelExchangeRate rate =
                exchangeRateRepository
                        .findFirstByHotel_IdAndCurrencyCodeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                hotelId,
                                currencyCode,
                                lookupTime
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No exchange rate is configured "
                                                + "for currency "
                                                + currencyCode
                                                + " at the requested time."));

        if (!rate.isEnabled()) {
            throw new IllegalStateException(
                    "Currency "
                            + currencyCode
                            + " is not enabled.");
        }

        return rate;
    }

    private Hotel getHotel(UUID hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Hotel not found: " + hotelId));
    }

    private BigDecimal normalizeRate(
            BigDecimal rate) {

        if (rate == null
                || rate.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Exchange rate must be greater than zero.");
        }

        return rate.setScale(
                EXCHANGE_RATE_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private void validateAmount(
            BigDecimal amount) {

        if (amount == null) {
            throw new IllegalArgumentException(
                    "Payment amount is required.");
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Payment amount must not be negative.");
        }
    }

    private String normalizeCurrencyCode(
            String currencyCode) {

        if (currencyCode == null) {
            throw new IllegalArgumentException(
                    "Currency code is required.");
        }

        String normalized =
                currencyCode
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (!normalized.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException(
                    "Currency code must contain "
                            + "exactly three letters.");
        }

        return normalized;
    }
}