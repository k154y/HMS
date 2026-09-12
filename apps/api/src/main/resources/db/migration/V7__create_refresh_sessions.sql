CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    token_hash VARCHAR(64) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,

    last_used_at TIMESTAMPTZ,

    revoked_at TIMESTAMPTZ,

    revocation_reason VARCHAR(100),

    replaced_by_session_id UUID,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT fk_refresh_sessions_replacement
        FOREIGN KEY (replaced_by_session_id)
        REFERENCES refresh_sessions (id),

    CONSTRAINT uq_refresh_sessions_token_hash
        UNIQUE (token_hash),

    CONSTRAINT chk_refresh_sessions_expiration
        CHECK (
            expires_at > created_at
        ),

    CONSTRAINT chk_refresh_sessions_revocation
        CHECK (
            revoked_at IS NOT NULL
            OR revocation_reason IS NULL
        )
);


CREATE INDEX idx_refresh_sessions_user
    ON refresh_sessions (user_id);


CREATE INDEX idx_refresh_sessions_user_active
    ON refresh_sessions (
        user_id,
        revoked_at,
        expires_at
    );


CREATE INDEX idx_refresh_sessions_expires_at
    ON refresh_sessions (expires_at);