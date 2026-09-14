CREATE TABLE payments (
 id UUID PRIMARY KEY, hotel_id UUID NOT NULL REFERENCES hotels(id), branch_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 folio_id UUID, cashier_user_id UUID NOT NULL REFERENCES users(id), method VARCHAR(30) NOT NULL CHECK(method IN ('CASH','MOBILE_MONEY','CARD','BANK_TRANSFER','CREDIT')),
 currency VARCHAR(3) NOT NULL, original_amount NUMERIC(19,4) NOT NULL CHECK(original_amount>0),
 fx_rate NUMERIC(19,8) NOT NULL CHECK(fx_rate>0), base_amount NUMERIC(19,4) NOT NULL CHECK(base_amount>0),
 external_reference VARCHAR(255), status VARCHAR(20) NOT NULL CHECK(status IN ('POSTED','VOIDED','REFUNDED')),
 idempotency_key VARCHAR(128) NOT NULL, actor_id UUID NOT NULL REFERENCES users(id),
 FOREIGN KEY(folio_id,hotel_id,branch_id) REFERENCES folios(id,hotel_id,branch_id),
 FOREIGN KEY(branch_id,hotel_id) REFERENCES branches(id,hotel_id),
 UNIQUE(hotel_id,branch_id,idempotency_key), UNIQUE(id,hotel_id,branch_id)
);
CREATE INDEX idx_payments_scope_time ON payments(hotel_id,branch_id,created_at);
CREATE TABLE cashier_shifts (
 id UUID PRIMARY KEY,hotel_id UUID NOT NULL REFERENCES hotels(id),branch_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 cashier_user_id UUID NOT NULL REFERENCES users(id), opened_at TIMESTAMPTZ NOT NULL, closed_at TIMESTAMPTZ,
 opening_float NUMERIC(19,4) NOT NULL CHECK(opening_float>=0), expected_amount NUMERIC(19,4),
 counted_amount NUMERIC(19,4), difference NUMERIC(19,4), status VARCHAR(20) NOT NULL CHECK(status IN ('OPEN','CLOSED','RECONCILED')),
 notes VARCHAR(1000), UNIQUE(id,hotel_id,branch_id), FOREIGN KEY(branch_id,hotel_id) REFERENCES branches(id,hotel_id)
);
CREATE UNIQUE INDEX uq_open_cashier_shift ON cashier_shifts(hotel_id,branch_id,cashier_user_id) WHERE status='OPEN';
CREATE TABLE purchase_orders (
 id UUID PRIMARY KEY,hotel_id UUID NOT NULL REFERENCES hotels(id),branch_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 vendor_id UUID NOT NULL, reference VARCHAR(100) NOT NULL, status VARCHAR(20) NOT NULL CHECK(status IN ('DRAFT','APPROVED','RECEIVED','CANCELLED')),
 total NUMERIC(19,4) NOT NULL CHECK(total>=0), actor_id UUID NOT NULL REFERENCES users(id),
 FOREIGN KEY(vendor_id,hotel_id) REFERENCES vendors(id,hotel_id), FOREIGN KEY(branch_id,hotel_id) REFERENCES branches(id,hotel_id),
 UNIQUE(hotel_id,reference), UNIQUE(id,hotel_id,branch_id)
);
