CREATE TABLE hotels (
    id UUID PRIMARY KEY,

    code VARCHAR(50) NOT NULL,

    legal_name VARCHAR(200) NOT NULL,

    display_name VARCHAR(200) NOT NULL,

    tin VARCHAR(100),

    phone VARCHAR(50),

    email VARCHAR(255),

    address TEXT,

    currency_code VARCHAR(3) NOT NULL DEFAULT 'RWF',

    timezone VARCHAR(100) NOT NULL DEFAULT 'Africa/Kigali',

    default_language VARCHAR(10) NOT NULL DEFAULT 'en',

    status VARCHAR(30) NOT NULL DEFAULT 'TRIAL',

    trial_started_at TIMESTAMPTZ NOT NULL,

    trial_ends_at TIMESTAMPTZ NOT NULL,

    grace_period_ends_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_hotels_code
        UNIQUE (code),

    CONSTRAINT chk_hotels_status
        CHECK (
            status IN (
                'TRIAL',
                'ACTIVE',
                'EXPIRED',
                'SUSPENDED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_hotels_default_language
        CHECK (
            default_language IN (
                'en',
                'fr',
                'rw'
            )
        ),

    CONSTRAINT chk_hotels_trial_dates
        CHECK (
            trial_ends_at > trial_started_at
        )
);


CREATE TABLE branches (
    id UUID PRIMARY KEY,

    hotel_id UUID NOT NULL,

    code VARCHAR(50) NOT NULL,

    name VARCHAR(200) NOT NULL,

    phone VARCHAR(50),

    email VARCHAR(255),

    address TEXT,

    timezone VARCHAR(100),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_branches_hotel
        FOREIGN KEY (hotel_id)
        REFERENCES hotels (id),

    CONSTRAINT uq_branches_hotel_code
        UNIQUE (hotel_id, code)
);


CREATE INDEX idx_branches_hotel_id
    ON branches (hotel_id);


CREATE INDEX idx_hotels_status
    ON hotels (status);


CREATE INDEX idx_hotels_trial_ends_at
    ON hotels (trial_ends_at);