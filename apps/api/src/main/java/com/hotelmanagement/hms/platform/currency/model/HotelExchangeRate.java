package com.hotelmanagement.hms.platform.currency.model;

import com.hotelmanagement.hms.platform.model.Hotel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "hotel_exchange_rates")
public class HotelExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(
            name = "base_currency_code",
            nullable = false,
            length = 3
    )
    private String baseCurrencyCode;

    @Column(
            name = "currency_code",
            nullable = false,
            length = 3
    )
    private String currencyCode;

    @Column(
            name = "rate_to_base",
            nullable = false,
            precision = 19,
            scale = 8
    )
    private BigDecimal rateToBase;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    protected HotelExchangeRate() {
        // Required by JPA.
    }

    /**
     * Creates a new immutable exchange-rate history record.
     *
     * Rate convention:
     *
     * 1 unit of currencyCode = rateToBase units of baseCurrencyCode.
     *
     * Example:
     * baseCurrencyCode = RWF
     * currencyCode = USD
     * rateToBase = 1450
     *
     * Therefore:
     * 1 USD = 1450 RWF.
     */
    public static HotelExchangeRate create(
            Hotel hotel,
            String baseCurrencyCode,
            String currencyCode,
            BigDecimal rateToBase,
            boolean enabled,
            OffsetDateTime effectiveFrom,
            OffsetDateTime createdAt) {

        HotelExchangeRate exchangeRate =
                new HotelExchangeRate();

        exchangeRate.hotel = hotel;
        exchangeRate.baseCurrencyCode =
                baseCurrencyCode;
        exchangeRate.currencyCode =
                currencyCode;
        exchangeRate.rateToBase =
                rateToBase;
        exchangeRate.enabled =
                enabled;
        exchangeRate.effectiveFrom =
                effectiveFrom;
        exchangeRate.createdAt =
                createdAt;

        return exchangeRate;
    }

    public UUID getId() {
        return id;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public String getBaseCurrencyCode() {
        return baseCurrencyCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getRateToBase() {
        return rateToBase;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public OffsetDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}