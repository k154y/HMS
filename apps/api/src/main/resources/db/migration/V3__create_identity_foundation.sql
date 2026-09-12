CREATE TABLE users (
    id UUID PRIMARY KEY,

    email VARCHAR(255) NOT NULL,

    normalized_email VARCHAR(255) NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    full_name VARCHAR(200) NOT NULL,

    phone VARCHAR(50),

    preferred_language VARCHAR(10) NOT NULL DEFAULT 'en',

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    failed_login_attempts INTEGER NOT NULL DEFAULT 0,

    locked_until TIMESTAMPTZ,

    last_login_at TIMESTAMPTZ,

    password_changed_at TIMESTAMPTZ NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_normalized_email
        UNIQUE (normalized_email),

    CONSTRAINT chk_users_preferred_language
        CHECK (
            preferred_language IN (
                'en',
                'fr',
                'rw'
            )
        ),

    CONSTRAINT chk_users_status
        CHECK (
            status IN (
                'ACTIVE',
                'LOCKED',
                'DISABLED'
            )
        ),

    CONSTRAINT chk_users_failed_login_attempts
        CHECK (
            failed_login_attempts >= 0
        )
);


CREATE INDEX idx_users_status
    ON users (status);


CREATE INDEX idx_users_locked_until
    ON users (locked_until);