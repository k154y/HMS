-- =========================================================
-- V27 - Managed hotel expense categories
-- =========================================================
--
-- Expense categories are hotel-level master data.
--
-- Actual expense transactions remain branch-level.
--
-- The existing expenses.category column is intentionally
-- retained as an immutable historical name snapshot.
--
-- New expenses additionally reference expense_categories.id.
--
-- Existing historical expense rows are NOT updated because
-- the expenses table is protected by an immutability trigger.
-- Their historical category text therefore remains untouched.
-- =========================================================


-- ---------------------------------------------------------
-- Expense permissions
-- ---------------------------------------------------------

INSERT INTO permissions (
    id,
    code,
    name,
    description
)
VALUES
(
    '10000000-0000-0000-0000-000000000065',
    'EXPENSE_VIEW',
    'View Expenses',
    'View expense transactions and available hotel expense categories.'
),
(
    '10000000-0000-0000-0000-000000000066',
    'EXPENSE_RECORD',
    'Record Expenses',
    'Record permitted hotel operating expense transactions.'
),
(
    '10000000-0000-0000-0000-000000000067',
    'EXPENSE_CATEGORY_MANAGE',
    'Manage Expense Categories',
    'Create, rename, activate, and deactivate hotel expense categories.'
)
ON CONFLICT (code) DO NOTHING;


-- ---------------------------------------------------------
-- Expense category master data
-- ---------------------------------------------------------

CREATE TABLE expense_categories (
    id UUID PRIMARY KEY,

    hotel_id UUID NOT NULL
        REFERENCES hotels(id),

    name VARCHAR(100) NOT NULL,

    active BOOLEAN NOT NULL
        DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_expense_categories_id_hotel
        UNIQUE (
            id,
            hotel_id
        ),

    CONSTRAINT chk_expense_categories_name
        CHECK (
            length(btrim(name)) > 0
        )
);


CREATE UNIQUE INDEX uq_expense_categories_hotel_name_ci
    ON expense_categories (
        hotel_id,
        lower(btrim(name))
    );


CREATE INDEX idx_expense_categories_hotel_active
    ON expense_categories (
        hotel_id,
        active
    );


-- ---------------------------------------------------------
-- Preserve existing category names as managed categories
-- ---------------------------------------------------------
--
-- If an existing hotel already has expense history such as:
--
--   Electricity
--   Transport
--   UTILITIES
--
-- those distinct names become available managed categories.
--
-- We DO NOT alter the historical expense transaction rows.
-- ---------------------------------------------------------

INSERT INTO expense_categories (
    id,
    hotel_id,
    name,
    active
)
SELECT
    gen_random_uuid(),
    grouped.hotel_id,
    grouped.name,
    TRUE
FROM (
    SELECT
        hotel_id,
        MIN(btrim(category)) AS name
    FROM expenses
    WHERE category IS NOT NULL
      AND length(btrim(category)) > 0
    GROUP BY
        hotel_id,
        lower(btrim(category))
) grouped
ON CONFLICT DO NOTHING;


-- ---------------------------------------------------------
-- Link new expenses to managed categories
-- ---------------------------------------------------------
--
-- Nullable intentionally:
--
-- Existing immutable historical rows do not have to be
-- rewritten merely to introduce the new relationship.
--
-- The application requires category_id for all NEW HTTP
-- expense transactions.
-- ---------------------------------------------------------

ALTER TABLE expenses
    ADD COLUMN category_id UUID;


ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_category_hotel
    FOREIGN KEY (
        category_id,
        hotel_id
    )
    REFERENCES expense_categories (
        id,
        hotel_id
    );


CREATE INDEX idx_expenses_category
    ON expenses (
        hotel_id,
        branch_id,
        category_id,
        expense_date
    );


-- ---------------------------------------------------------
-- Existing hotel system roles
-- ---------------------------------------------------------


-- Owner, Manager and Accountant:
-- view + record + manage categories.

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
      'MANAGER',
      'ACCOUNTANT'
  )
  AND p.code IN (
      'EXPENSE_VIEW',
      'EXPENSE_RECORD',
      'EXPENSE_CATEGORY_MANAGE'
  )
ON CONFLICT DO NOTHING;


-- Cashier:
-- may see expense categories and record expenses,
-- but may not manage category master data.

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
  AND r.code = 'CASHIER'
  AND p.code IN (
      'EXPENSE_VIEW',
      'EXPENSE_RECORD'
  )
ON CONFLICT DO NOTHING;


-- Auditor:
-- preserve read-only financial oversight.

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
  AND r.code = 'AUDITOR'
  AND p.code = 'EXPENSE_VIEW'
ON CONFLICT DO NOTHING;