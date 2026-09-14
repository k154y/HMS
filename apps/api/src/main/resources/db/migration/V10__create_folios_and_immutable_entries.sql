CREATE TABLE folios (
 id UUID PRIMARY KEY, hotel_id UUID NOT NULL REFERENCES hotels(id), branch_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0, customer_id UUID NOT NULL,
 currency VARCHAR(3) NOT NULL, status VARCHAR(20) NOT NULL CHECK(status IN ('OPEN','CLOSED','CREDIT')),
 FOREIGN KEY(customer_id,hotel_id) REFERENCES customers(id,hotel_id),
 FOREIGN KEY(branch_id,hotel_id) REFERENCES branches(id,hotel_id), UNIQUE(id,hotel_id,branch_id)
);
CREATE INDEX idx_folios_customer ON folios(hotel_id,customer_id);
CREATE INDEX idx_folios_scope ON folios(hotel_id,branch_id,created_at,id);
CREATE TABLE folio_entries (
 id UUID PRIMARY KEY, hotel_id UUID NOT NULL, branch_id UUID NOT NULL, folio_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 amount NUMERIC(19,4) NOT NULL CHECK(amount<>0),
 kind VARCHAR(30) NOT NULL CHECK(kind IN ('CHARGE','ACCOMMODATION','ORDER','PAYMENT','REFUND','REVERSAL','TRANSFER_IN','TRANSFER_OUT')),
 source_id UUID NOT NULL, memo VARCHAR(1000) NOT NULL, actor_id UUID NOT NULL REFERENCES users(id),
 FOREIGN KEY(folio_id,hotel_id,branch_id) REFERENCES folios(id,hotel_id,branch_id),
 UNIQUE(id,hotel_id,branch_id), UNIQUE(folio_id,kind,source_id)
);
CREATE INDEX idx_folio_entries_balance ON folio_entries(folio_id,hotel_id,branch_id);
CREATE INDEX idx_folio_entries_actor ON folio_entries(actor_id);
CREATE TRIGGER folio_entries_immutable BEFORE UPDATE OR DELETE ON folio_entries FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
CREATE TRIGGER folios_no_delete BEFORE DELETE ON folios FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
