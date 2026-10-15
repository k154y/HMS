-- Parent commands and their effects commit in the same service transaction.
-- Do not guess parent identities for historical payment approvals.
CREATE TABLE inventory_transfer_commands (
 hotel_id UUID NOT NULL REFERENCES hotels(id), request_id UUID NOT NULL,
 source_branch_id UUID NOT NULL, destination_branch_id UUID NOT NULL, product_id UUID NOT NULL,
 quantity NUMERIC(19,4) NOT NULL CHECK(quantity > 0), reason VARCHAR(1000) NOT NULL,
 actor_id UUID NOT NULL REFERENCES users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(hotel_id,request_id), CHECK(source_branch_id <> destination_branch_id),
 FOREIGN KEY(source_branch_id,hotel_id) REFERENCES branches(id,hotel_id),
 FOREIGN KEY(destination_branch_id,hotel_id) REFERENCES branches(id,hotel_id),
 FOREIGN KEY(product_id,hotel_id) REFERENCES products(id,hotel_id)
);
CREATE TRIGGER inventory_transfer_commands_immutable BEFORE UPDATE OR DELETE ON inventory_transfer_commands
 FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
CREATE INDEX idx_stock_transfer_request ON stock_movements(hotel_id,source_id)
 WHERE kind IN ('TRANSFER_IN','TRANSFER_OUT');

CREATE TABLE credit_settlement_commands (
 hotel_id UUID NOT NULL REFERENCES hotels(id), request_id UUID NOT NULL,
 branch_id UUID NOT NULL, customer_id UUID NOT NULL,
 amount NUMERIC(19,4) NOT NULL CHECK(amount > 0), description VARCHAR(1000) NOT NULL,
 response_balance NUMERIC NOT NULL, actor_id UUID NOT NULL REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(hotel_id,request_id), UNIQUE(hotel_id,branch_id,request_id),
 FOREIGN KEY(branch_id,hotel_id) REFERENCES branches(id,hotel_id),
 FOREIGN KEY(customer_id,hotel_id) REFERENCES customers(id,hotel_id)
);
CREATE TABLE credit_settlement_allocations (
 hotel_id UUID NOT NULL, branch_id UUID NOT NULL, request_id UUID NOT NULL,
 folio_id UUID NOT NULL, amount NUMERIC(19,4) NOT NULL CHECK(amount > 0),
 payment_request_id UUID NOT NULL, payment_approval_id UUID NOT NULL UNIQUE REFERENCES payment_approvals(id),
 PRIMARY KEY(hotel_id,request_id,folio_id), UNIQUE(hotel_id,branch_id,payment_request_id),
 FOREIGN KEY(hotel_id,branch_id,request_id) REFERENCES credit_settlement_commands(hotel_id,branch_id,request_id),
 FOREIGN KEY(folio_id,hotel_id,branch_id) REFERENCES folios(id,hotel_id,branch_id)
);
CREATE TRIGGER credit_settlement_commands_immutable BEFORE UPDATE OR DELETE ON credit_settlement_commands
 FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
CREATE TRIGGER credit_settlement_allocations_immutable BEFORE UPDATE OR DELETE ON credit_settlement_allocations
 FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
