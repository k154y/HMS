-- ============================================================
-- V28 - Non-resident customer billing
-- ============================================================
--
-- A non-resident bill represents hotel sales to customers who
-- are not using a room/stay.
--
-- The bill has its own folio so that the existing immutable
-- financial ledger, payment workflow and credit workflow can
-- continue to be authoritative.
--
-- A non-resident bill does NOT require:
--   - reservation
--   - room
--   - stay
--   - check-in
-- ============================================================


-- ------------------------------------------------------------
-- Permissions
-- ------------------------------------------------------------

INSERT INTO permissions (
    id,
    code,
    name,
    description
)
VALUES
(
    '10000000-0000-0000-0000-000000000068',
    'NON_RESIDENT_BILL_VIEW',
    'View Non-Resident Bills',
    'View non-resident customer bills and their balances.'
),
(
    '10000000-0000-0000-0000-000000000069',
    'NON_RESIDENT_BILL_CREATE',
    'Create Non-Resident Bills',
    'Open bills for walk-in and other non-resident customers.'
),
(
    '10000000-0000-0000-0000-000000000070',
    'NON_RESIDENT_BILL_CANCEL',
    'Cancel Non-Resident Bills',
    'Cancel an unused non-resident bill before financial activity has been posted.'
),
(
    '10000000-0000-0000-0000-000000000071',
    'NON_RESIDENT_BILL_VOID',
    'Void Non-Resident Bills',
    'Void a non-resident bill and reverse its permitted financial activity.'
)
ON CONFLICT (code) DO NOTHING;


-- ------------------------------------------------------------
-- Non-resident bill master
-- ------------------------------------------------------------

CREATE TABLE non_resident_bills (
    id UUID PRIMARY KEY,

    hotel_id UUID NOT NULL,
    branch_id UUID NOT NULL,

    folio_id UUID NOT NULL,
    customer_id UUID NOT NULL,

    reference VARCHAR(100) NOT NULL,

    bill_type VARCHAR(30) NOT NULL
        CHECK (
            bill_type IN (
                'RESTAURANT',
                'BAR',
                'LAUNDRY',
                'TRANSPORT',
                'SWIMMING_POOL',
                'DAY_USE',
                'EVENT',
                'CONFERENCE',
                'OUTSIDE_CATERING',
                'OTHER'
            )
        ),

    table_reference VARCHAR(100),

    notes VARCHAR(1000),

    created_by UUID NOT NULL
        REFERENCES users(id),

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    cancelled_at TIMESTAMPTZ,

    cancelled_by UUID
        REFERENCES users(id),

    cancellation_reason VARCHAR(1000),

    voided_at TIMESTAMPTZ,

    voided_by UUID
        REFERENCES users(id),

    void_reason VARCHAR(1000),

    CONSTRAINT uq_non_resident_bill_tenant
        UNIQUE (
            id,
            hotel_id,
            branch_id
        ),

    CONSTRAINT uq_non_resident_bill_reference
        UNIQUE (
            hotel_id,
            branch_id,
            reference
        ),

    CONSTRAINT uq_non_resident_bill_folio
        UNIQUE (
            folio_id
        ),

    CONSTRAINT fk_non_resident_bill_branch
        FOREIGN KEY (
            branch_id,
            hotel_id
        )
        REFERENCES branches (
            id,
            hotel_id
        ),

    CONSTRAINT fk_non_resident_bill_customer
        FOREIGN KEY (
            customer_id,
            hotel_id
        )
        REFERENCES customers (
            id,
            hotel_id
        ),

    CONSTRAINT fk_non_resident_bill_folio
        FOREIGN KEY (
            folio_id,
            hotel_id,
            branch_id
        )
        REFERENCES folios (
            id,
            hotel_id,
            branch_id
        ),

    CONSTRAINT chk_non_resident_bill_cancel
        CHECK (
            (
                cancelled_at IS NULL
                AND cancelled_by IS NULL
                AND cancellation_reason IS NULL
            )
            OR
            (
                cancelled_at IS NOT NULL
                AND cancelled_by IS NOT NULL
                AND cancellation_reason IS NOT NULL
            )
        ),

    CONSTRAINT chk_non_resident_bill_void
        CHECK (
            (
                voided_at IS NULL
                AND voided_by IS NULL
                AND void_reason IS NULL
            )
            OR
            (
                voided_at IS NOT NULL
                AND voided_by IS NOT NULL
                AND void_reason IS NOT NULL
            )
        ),

    CONSTRAINT chk_non_resident_bill_terminal_state
        CHECK (
            NOT (
                cancelled_at IS NOT NULL
                AND voided_at IS NOT NULL
            )
        )
);


CREATE INDEX idx_non_resident_bills_branch_created
    ON non_resident_bills (
        hotel_id,
        branch_id,
        created_at DESC
    );


CREATE INDEX idx_non_resident_bills_customer
    ON non_resident_bills (
        hotel_id,
        customer_id,
        created_at DESC
    );


CREATE INDEX idx_non_resident_bills_type
    ON non_resident_bills (
        hotel_id,
        branch_id,
        bill_type,
        created_at DESC
    );


-- ------------------------------------------------------------
-- Permissions for already-created hotel roles
-- ------------------------------------------------------------

-- Owner / Manager:
-- full non-resident billing authority.

INSERT INTO role_permissions (
    role_id,
    permission_id
)
SELECT
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.system_defined
  AND r.code IN (
      'OWNER',
      'MANAGER'
  )
  AND p.code IN (
      'NON_RESIDENT_BILL_VIEW',
      'NON_RESIDENT_BILL_CREATE',
      'NON_RESIDENT_BILL_CANCEL',
      'NON_RESIDENT_BILL_VOID'
  )
ON CONFLICT DO NOTHING;


-- Supervisor:
-- operational control including cancellation / void authority.

INSERT INTO role_permissions (
    role_id,
    permission_id
)
SELECT
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.system_defined
  AND r.code = 'SUPERVISOR'
  AND p.code IN (
      'NON_RESIDENT_BILL_VIEW',
      'NON_RESIDENT_BILL_CREATE',
      'NON_RESIDENT_BILL_CANCEL',
      'NON_RESIDENT_BILL_VOID'
  )
ON CONFLICT DO NOTHING;


-- Receptionist / Cashier / Waiter:
-- may open and work with non-resident bills.
--
-- They may not cancel or void bills without elevated authority.

INSERT INTO role_permissions (
    role_id,
    permission_id
)
SELECT
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.system_defined
  AND r.code IN (
      'RECEPTIONIST',
      'CASHIER',
      'WAITER'
  )
  AND p.code IN (
      'NON_RESIDENT_BILL_VIEW',
      'NON_RESIDENT_BILL_CREATE'
  )
ON CONFLICT DO NOTHING;


-- Accountant / Auditor:
-- read-only visibility.

INSERT INTO role_permissions (
    role_id,
    permission_id
)
SELECT
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.system_defined
  AND r.code IN (
      'ACCOUNTANT',
      'AUDITOR'
  )
  AND p.code = 'NON_RESIDENT_BILL_VIEW'
ON CONFLICT DO NOTHING;