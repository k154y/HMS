CREATE TABLE hotel_exchange_rates (
    id UUID PRIMARY KEY,

    hotel_id UUID NOT NULL,

    base_currency_code VARCHAR(3) NOT NULL,

    currency_code VARCHAR(3) NOT NULL,

    rate_to_base NUMERIC(19, 8) NOT NULL,

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    effective_from TIMESTAMPTZ NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_hotel_exchange_rates_hotel
        FOREIGN KEY (hotel_id)
        REFERENCES hotels (id),

    CONSTRAINT chk_hotel_exchange_rates_base_currency
        CHECK (
            base_currency_code ~ '^[A-Z]{3}$'
        ),

    CONSTRAINT chk_hotel_exchange_rates_currency
        CHECK (
            currency_code ~ '^[A-Z]{3}$'
        ),

    CONSTRAINT chk_hotel_exchange_rates_different_currency
        CHECK (
            base_currency_code <> currency_code
        ),

    CONSTRAINT chk_hotel_exchange_rates_positive_rate
        CHECK (
            rate_to_base > 0
        ),

    CONSTRAINT uq_hotel_exchange_rates_effective
        UNIQUE (
            hotel_id,
            currency_code,
            effective_from
        )
);


CREATE INDEX idx_hotel_exchange_rates_lookup
    ON hotel_exchange_rates (
        hotel_id,
        currency_code,
        effective_from DESC
    );


CREATE INDEX idx_hotel_exchange_rates_hotel
    ON hotel_exchange_rates (hotel_id);