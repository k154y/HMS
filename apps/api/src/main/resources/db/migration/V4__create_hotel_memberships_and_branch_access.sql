ALTER TABLE branches
    ADD CONSTRAINT uq_branches_id_hotel
        UNIQUE (id, hotel_id);


CREATE TABLE hotel_memberships (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    hotel_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    all_branches BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_hotel_memberships_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT fk_hotel_memberships_hotel
        FOREIGN KEY (hotel_id)
        REFERENCES hotels (id),

    CONSTRAINT uq_hotel_memberships_user_hotel
        UNIQUE (user_id, hotel_id),

    CONSTRAINT uq_hotel_memberships_id_hotel
        UNIQUE (id, hotel_id),

    CONSTRAINT chk_hotel_memberships_status
        CHECK (
            status IN (
                'ACTIVE',
                'SUSPENDED',
                'REVOKED'
            )
        )
);


CREATE INDEX idx_hotel_memberships_user
    ON hotel_memberships (user_id);


CREATE INDEX idx_hotel_memberships_hotel
    ON hotel_memberships (hotel_id);


CREATE INDEX idx_hotel_memberships_hotel_status
    ON hotel_memberships (
        hotel_id,
        status
    );


CREATE TABLE membership_branch_access (
    id UUID PRIMARY KEY,

    membership_id UUID NOT NULL,

    hotel_id UUID NOT NULL,

    branch_id UUID NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_membership_branch_access_membership
        FOREIGN KEY (
            membership_id,
            hotel_id
        )
        REFERENCES hotel_memberships (
            id,
            hotel_id
        ),

    CONSTRAINT fk_membership_branch_access_branch
        FOREIGN KEY (
            branch_id,
            hotel_id
        )
        REFERENCES branches (
            id,
            hotel_id
        ),

    CONSTRAINT uq_membership_branch_access
        UNIQUE (
            membership_id,
            branch_id
        )
);


CREATE INDEX idx_membership_branch_access_membership
    ON membership_branch_access (membership_id);


CREATE INDEX idx_membership_branch_access_branch
    ON membership_branch_access (branch_id);


CREATE INDEX idx_membership_branch_access_hotel
    ON membership_branch_access (hotel_id);