CREATE TABLE permissions (
    id UUID PRIMARY KEY,

    code VARCHAR(100) NOT NULL,

    name VARCHAR(150) NOT NULL,

    description TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_permissions_code
        UNIQUE (code),

    CONSTRAINT chk_permissions_code
        CHECK (
            code ~ '^[A-Z][A-Z0-9_]*$'
        )
);


CREATE TABLE roles (
    id UUID PRIMARY KEY,

    hotel_id UUID NOT NULL,

    code VARCHAR(100) NOT NULL,

    name VARCHAR(150) NOT NULL,

    description TEXT,

    system_defined BOOLEAN NOT NULL DEFAULT FALSE,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_roles_hotel
        FOREIGN KEY (hotel_id)
        REFERENCES hotels (id),

    CONSTRAINT uq_roles_hotel_code
        UNIQUE (
            hotel_id,
            code
        ),

    CONSTRAINT uq_roles_id_hotel
        UNIQUE (
            id,
            hotel_id
        ),

    CONSTRAINT chk_roles_code
        CHECK (
            code ~ '^[A-Z][A-Z0-9_]*$'
        )
);


CREATE INDEX idx_roles_hotel
    ON roles (hotel_id);


CREATE INDEX idx_roles_hotel_active
    ON roles (
        hotel_id,
        active
    );


CREATE TABLE role_permissions (
    role_id UUID NOT NULL,

    permission_id UUID NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_role_permissions
        PRIMARY KEY (
            role_id,
            permission_id
        ),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES roles (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions (id)
);


CREATE INDEX idx_role_permissions_permission
    ON role_permissions (permission_id);


CREATE TABLE membership_roles (
    id UUID PRIMARY KEY,

    membership_id UUID NOT NULL,

    hotel_id UUID NOT NULL,

    role_id UUID NOT NULL,

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_membership_roles_membership
        FOREIGN KEY (
            membership_id,
            hotel_id
        )
        REFERENCES hotel_memberships (
            id,
            hotel_id
        ),

    CONSTRAINT fk_membership_roles_role
        FOREIGN KEY (
            role_id,
            hotel_id
        )
        REFERENCES roles (
            id,
            hotel_id
        ),

    CONSTRAINT uq_membership_roles_membership_role
        UNIQUE (
            membership_id,
            role_id
        )
);


CREATE INDEX idx_membership_roles_membership
    ON membership_roles (membership_id);


CREATE INDEX idx_membership_roles_role
    ON membership_roles (role_id);


CREATE INDEX idx_membership_roles_hotel
    ON membership_roles (hotel_id);