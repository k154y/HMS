-- Commerce-only financial foundation. Historical hotel records are not touched.
ALTER TABLE commerce_products ADD COLUMN description VARCHAR(2000);
ALTER TABLE commerce_products ADD COLUMN minimum_price NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK(minimum_price>=0);
ALTER TABLE commerce_products ADD COLUMN tax_rate NUMERIC(7,4) NOT NULL DEFAULT 0 CHECK(tax_rate BETWEEN 0 AND 100);
ALTER TABLE commerce_products ADD COLUMN tax_inclusive BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE commerce_products ADD COLUMN purchase_tax_recoverable BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE commerce_products ADD COLUMN low_stock_quantity NUMERIC(19,6) NOT NULL DEFAULT 0 CHECK(low_stock_quantity>=0);
ALTER TABLE commerce_products ALTER COLUMN wholesale_price DROP NOT NULL;
CREATE TABLE commerce_branch_prices (
 id UUID PRIMARY KEY,business_id UUID NOT NULL,branch_id UUID NOT NULL,product_id UUID NOT NULL,
 price NUMERIC(19,4) NOT NULL CHECK(price>=0),effective_at TIMESTAMPTZ NOT NULL,
 created_by UUID NOT NULL REFERENCES users(id),created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(business_id,branch_id) REFERENCES commerce_branches(business_id,id),
 FOREIGN KEY(business_id,product_id) REFERENCES commerce_products(business_id,id),
 UNIQUE(business_id,branch_id,product_id,effective_at)
);
CREATE TRIGGER commerce_branch_prices_immutable BEFORE UPDATE OR DELETE ON commerce_branch_prices
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
CREATE TABLE commerce_parties (
 id UUID PRIMARY KEY,business_id UUID NOT NULL REFERENCES commerce_businesses(id),
 kind VARCHAR(20) NOT NULL CHECK(kind IN ('CUSTOMER','SUPPLIER')),name VARCHAR(200) NOT NULL,
 contact VARCHAR(300),credit_limit_base NUMERIC(24,6) NOT NULL DEFAULT 0 CHECK(credit_limit_base>=0),
 active BOOLEAN NOT NULL DEFAULT TRUE,UNIQUE(business_id,id)
);
CREATE TABLE commerce_payment_accounts (
 id UUID PRIMARY KEY,business_id UUID NOT NULL REFERENCES commerce_businesses(id),name VARCHAR(200) NOT NULL,
 method VARCHAR(20) NOT NULL CHECK(method IN ('CASH','BANK','MOBILE_MONEY','CARD')),
 currency VARCHAR(3) NOT NULL,reference VARCHAR(200),active BOOLEAN NOT NULL DEFAULT TRUE,
 UNIQUE(business_id,id),UNIQUE(business_id,name),CHECK(currency ~ '^[A-Z]{3}$')
);
CREATE TABLE commerce_financial_commands (
 id UUID PRIMARY KEY,business_id UUID NOT NULL REFERENCES commerce_businesses(id),request_id UUID NOT NULL,
 kind VARCHAR(40) NOT NULL,actor_id UUID NOT NULL REFERENCES users(id),payload JSONB NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE(business_id,request_id),UNIQUE(business_id,id)
);
CREATE TRIGGER commerce_financial_commands_immutable BEFORE UPDATE OR DELETE ON commerce_financial_commands
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
CREATE TABLE commerce_accounting_state (
 business_id UUID PRIMARY KEY REFERENCES commerce_businesses(id),activation_id UUID NOT NULL,
 created_by UUID NOT NULL REFERENCES users(id),created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(business_id,activation_id) REFERENCES commerce_financial_commands(business_id,id)
);
CREATE TRIGGER commerce_accounting_state_immutable BEFORE UPDATE OR DELETE ON commerce_accounting_state
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
ALTER TABLE commerce_stock_balances ADD COLUMN inventory_value NUMERIC(24,6) NOT NULL DEFAULT 0 CHECK(inventory_value>=0);

CREATE TABLE commerce_journals (
 id UUID PRIMARY KEY,business_id UUID NOT NULL REFERENCES commerce_businesses(id),source_kind VARCHAR(40) NOT NULL,
 source_id UUID NOT NULL,description VARCHAR(500) NOT NULL,created_by UUID NOT NULL REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,created_txid BIGINT NOT NULL DEFAULT txid_current(),
 UNIQUE(business_id,id),UNIQUE(business_id,source_kind,source_id)
);
CREATE TABLE commerce_journal_lines (
 id UUID PRIMARY KEY,business_id UUID NOT NULL,journal_id UUID NOT NULL,branch_id UUID NOT NULL,
 account VARCHAR(40) NOT NULL CHECK(account IN ('INVENTORY','OPENING_EQUITY','STOCK_LOSS','STOCK_GAIN','INTERBRANCH',
 'CASH_BANK','RECEIVABLE','PAYABLE','GOODS_CLEARING','INPUT_TAX','OUTPUT_TAX','SALES','COGS','FX_GAIN','FX_LOSS','PURCHASE_VARIANCE','EXPENSE')),
 debit NUMERIC(24,6) NOT NULL DEFAULT 0 CHECK(debit>=0),credit NUMERIC(24,6) NOT NULL DEFAULT 0 CHECK(credit>=0),
 payment_account_id UUID,party_id UUID,
 FOREIGN KEY(business_id,journal_id) REFERENCES commerce_journals(business_id,id),
 FOREIGN KEY(business_id,branch_id) REFERENCES commerce_branches(business_id,id),
 FOREIGN KEY(business_id,payment_account_id) REFERENCES commerce_payment_accounts(business_id,id),
 FOREIGN KEY(business_id,party_id) REFERENCES commerce_parties(business_id,id),
 CHECK((debit>0 AND credit=0) OR (credit>0 AND debit=0))
);
CREATE OR REPLACE FUNCTION commerce_check_balanced_journal() RETURNS trigger AS $$
DECLARE target UUID;
BEGIN
 target := CASE WHEN TG_TABLE_NAME='commerce_journals' THEN NEW.id ELSE NEW.journal_id END;
 IF EXISTS(SELECT 1 FROM commerce_journal_lines WHERE journal_id=target GROUP BY branch_id HAVING SUM(debit)<>SUM(credit)) THEN
  RAISE EXCEPTION 'Commerce journal must balance within every branch';
 END IF;
 RETURN NULL;
END;
$$ LANGUAGE plpgsql;
CREATE CONSTRAINT TRIGGER commerce_journal_balanced AFTER INSERT ON commerce_journals
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION commerce_check_balanced_journal();
CREATE CONSTRAINT TRIGGER commerce_lines_balanced AFTER INSERT ON commerce_journal_lines
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION commerce_check_balanced_journal();
CREATE TRIGGER commerce_journals_immutable BEFORE UPDATE OR DELETE ON commerce_journals
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
CREATE TRIGGER commerce_journal_lines_immutable BEFORE UPDATE OR DELETE ON commerce_journal_lines
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
CREATE INDEX commerce_journal_reporting ON commerce_journal_lines(business_id,branch_id,account);
CREATE OR REPLACE FUNCTION commerce_guard_journal_append() RETURNS trigger AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM commerce_journals WHERE id=NEW.journal_id AND created_txid=txid_current()) THEN
  RAISE EXCEPTION 'Cannot append to a posted journal';
 END IF;
 RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER commerce_journal_no_late_lines BEFORE INSERT ON commerce_journal_lines
 FOR EACH ROW EXECUTE FUNCTION commerce_guard_journal_append();
CREATE OR REPLACE FUNCTION commerce_guard_base_currency() RETURNS trigger AS $$
BEGIN
 IF NEW.base_currency<>OLD.base_currency AND EXISTS(SELECT 1 FROM commerce_accounting_state WHERE business_id=OLD.id) THEN
  RAISE EXCEPTION 'Cannot change base currency after accounting activation';
 END IF;
 RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER commerce_fixed_accounting_currency BEFORE UPDATE ON commerce_businesses
 FOR EACH ROW EXECUTE FUNCTION commerce_guard_base_currency();

CREATE TABLE commerce_shifts (
 id UUID PRIMARY KEY,business_id UUID NOT NULL,branch_id UUID NOT NULL,cashier_id UUID NOT NULL REFERENCES users(id),
 payment_account_id UUID NOT NULL,opening_cash NUMERIC(24,6) NOT NULL CHECK(opening_cash>=0),
 opened_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,closed_at TIMESTAMPTZ,counted_cash NUMERIC(24,6),
 expected_cash NUMERIC(24,6),variance NUMERIC(24,6),approved_by UUID REFERENCES users(id),approved_at TIMESTAMPTZ,
 UNIQUE(business_id,id),FOREIGN KEY(business_id,branch_id) REFERENCES commerce_branches(business_id,id),
 FOREIGN KEY(business_id,payment_account_id) REFERENCES commerce_payment_accounts(business_id,id)
);
CREATE UNIQUE INDEX commerce_one_open_shift ON commerce_shifts(business_id,cashier_id) WHERE closed_at IS NULL;
CREATE TABLE commerce_documents (
 id UUID PRIMARY KEY,business_id UUID NOT NULL,branch_id UUID NOT NULL,location_id UUID NOT NULL,
 kind VARCHAR(20) NOT NULL CHECK(kind IN ('SALE','PURCHASE','EXPENSE')),
 document_number BIGSERIAL NOT NULL UNIQUE,party_id UUID,party_name VARCHAR(200),reference VARCHAR(200),due_date DATE,
 currency VARCHAR(3) NOT NULL,exchange_rate NUMERIC(24,10) NOT NULL CHECK(exchange_rate>0),rate_id UUID,
 subtotal NUMERIC(24,6) NOT NULL CHECK(subtotal>=0),tax NUMERIC(24,6) NOT NULL CHECK(tax>=0),
 total NUMERIC(24,6) NOT NULL CHECK(total>0),base_total NUMERIC(24,6) NOT NULL CHECK(base_total>0),
 created_by UUID NOT NULL REFERENCES users(id),created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(business_id,id),
 FOREIGN KEY(business_id,id) REFERENCES commerce_financial_commands(business_id,id),
 FOREIGN KEY(business_id,branch_id,location_id) REFERENCES commerce_stock_locations(business_id,branch_id,id),
 FOREIGN KEY(business_id,party_id) REFERENCES commerce_parties(business_id,id),
 CHECK(total=subtotal+tax)
);
CREATE TABLE commerce_document_lines (
 id UUID PRIMARY KEY,business_id UUID NOT NULL,document_id UUID NOT NULL,position INTEGER NOT NULL,
 product_id UUID NOT NULL,sku VARCHAR(80) NOT NULL,description VARCHAR(200) NOT NULL,unit VARCHAR(32) NOT NULL,
 quantity NUMERIC(19,6) NOT NULL CHECK(quantity>0),base_quantity NUMERIC(19,6) NOT NULL CHECK(base_quantity>0),
 standard_price NUMERIC(24,6) NOT NULL CHECK(standard_price>=0),unit_price NUMERIC(24,6) NOT NULL CHECK(unit_price>=0),
 discount NUMERIC(24,6) NOT NULL DEFAULT 0 CHECK(discount>=0),discount_reason VARCHAR(500),
 tax_rate NUMERIC(7,4) NOT NULL CHECK(tax_rate BETWEEN 0 AND 100),tax_recoverable BOOLEAN NOT NULL,
 net NUMERIC(24,6) NOT NULL CHECK(net>=0),tax NUMERIC(24,6) NOT NULL CHECK(tax>=0),
 base_net NUMERIC(24,6) NOT NULL CHECK(base_net>=0),base_tax NUMERIC(24,6) NOT NULL CHECK(base_tax>=0),
 inventory_value NUMERIC(24,6) NOT NULL DEFAULT 0 CHECK(inventory_value>=0),
 UNIQUE(business_id,id),UNIQUE(document_id,position),
 FOREIGN KEY(business_id,document_id) REFERENCES commerce_documents(business_id,id),
 FOREIGN KEY(business_id,product_id) REFERENCES commerce_products(business_id,id)
);
CREATE TRIGGER commerce_documents_immutable BEFORE UPDATE OR DELETE ON commerce_documents
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
CREATE TRIGGER commerce_document_lines_immutable BEFORE UPDATE OR DELETE ON commerce_document_lines
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
CREATE TABLE commerce_receipt_lines (
 id UUID PRIMARY KEY,business_id UUID NOT NULL,command_id UUID NOT NULL,line_id UUID NOT NULL,
 quantity NUMERIC(19,6) NOT NULL CHECK(quantity>0),inventory_value NUMERIC(24,6) NOT NULL CHECK(inventory_value>=0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(business_id,command_id) REFERENCES commerce_financial_commands(business_id,id),
 FOREIGN KEY(business_id,line_id) REFERENCES commerce_document_lines(business_id,id),UNIQUE(command_id,line_id)
);
CREATE TRIGGER commerce_receipt_lines_immutable BEFORE UPDATE OR DELETE ON commerce_receipt_lines
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
CREATE TABLE commerce_payments (
 id UUID PRIMARY KEY,business_id UUID NOT NULL,command_id UUID NOT NULL,document_id UUID NOT NULL,
 payment_account_id UUID NOT NULL,shift_id UUID,reference VARCHAR(200),refund BOOLEAN NOT NULL DEFAULT FALSE,
 settlement_document_rate NUMERIC(24,10) NOT NULL CHECK(settlement_document_rate>0),
 currency VARCHAR(3) NOT NULL,amount NUMERIC(24,6) NOT NULL CHECK(amount>0),
 exchange_rate NUMERIC(24,10) NOT NULL CHECK(exchange_rate>0),rate_id UUID,
 document_amount NUMERIC(24,6) NOT NULL CHECK(document_amount>0),base_amount NUMERIC(24,6) NOT NULL CHECK(base_amount>0),
 carrying_amount NUMERIC(24,6) NOT NULL CHECK(carrying_amount>=0),fx_difference NUMERIC(24,6) NOT NULL,
 created_by UUID NOT NULL REFERENCES users(id),created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(business_id,command_id) REFERENCES commerce_financial_commands(business_id,id),
 FOREIGN KEY(business_id,document_id) REFERENCES commerce_documents(business_id,id),
 FOREIGN KEY(business_id,payment_account_id) REFERENCES commerce_payment_accounts(business_id,id),
 FOREIGN KEY(business_id,shift_id) REFERENCES commerce_shifts(business_id,id)
);
CREATE TRIGGER commerce_payments_immutable BEFORE UPDATE OR DELETE ON commerce_payments
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
CREATE TABLE commerce_returns (
 id UUID PRIMARY KEY,business_id UUID NOT NULL,command_id UUID NOT NULL,line_id UUID NOT NULL,
 quantity NUMERIC(19,6) NOT NULL CHECK(quantity>0),saleable BOOLEAN NOT NULL,
 document_amount NUMERIC(24,6) NOT NULL CHECK(document_amount>=0),base_net NUMERIC(24,6) NOT NULL,
 base_tax NUMERIC(24,6) NOT NULL,inventory_value NUMERIC(24,6) NOT NULL,reason VARCHAR(500) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(business_id,command_id) REFERENCES commerce_financial_commands(business_id,id),
 FOREIGN KEY(business_id,line_id) REFERENCES commerce_document_lines(business_id,id),UNIQUE(command_id,line_id)
);
CREATE TRIGGER commerce_returns_immutable BEFORE UPDATE OR DELETE ON commerce_returns
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
CREATE TABLE commerce_cash_moves (
 id UUID PRIMARY KEY,business_id UUID NOT NULL,shift_id UUID NOT NULL,amount NUMERIC(24,6) NOT NULL CHECK(amount<>0),
 reason VARCHAR(500) NOT NULL,created_by UUID NOT NULL REFERENCES users(id),created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(business_id,id) REFERENCES commerce_financial_commands(business_id,id),
 FOREIGN KEY(business_id,shift_id) REFERENCES commerce_shifts(business_id,id)
);
CREATE TRIGGER commerce_cash_moves_immutable BEFORE UPDATE OR DELETE ON commerce_cash_moves
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
CREATE TABLE commerce_expenses (
 id UUID PRIMARY KEY,business_id UUID NOT NULL,branch_id UUID NOT NULL,payment_account_id UUID NOT NULL,
 currency VARCHAR(3) NOT NULL,amount NUMERIC(24,6) NOT NULL CHECK(amount>0),exchange_rate NUMERIC(24,10) NOT NULL CHECK(exchange_rate>0),
 rate_id UUID,base_amount NUMERIC(24,6) NOT NULL CHECK(base_amount>0),reason VARCHAR(500) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(business_id,id) REFERENCES commerce_financial_commands(business_id,id),
 FOREIGN KEY(business_id,branch_id) REFERENCES commerce_branches(business_id,id),
 FOREIGN KEY(business_id,payment_account_id) REFERENCES commerce_payment_accounts(business_id,id)
);
CREATE TRIGGER commerce_expenses_immutable BEFORE UPDATE OR DELETE ON commerce_expenses
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
CREATE OR REPLACE FUNCTION commerce_guard_closed_shift() RETURNS trigger AS $$
BEGIN
 IF TG_OP='DELETE' OR (OLD.closed_at IS NOT NULL AND
 (to_jsonb(NEW)-'approved_by'-'approved_at') IS DISTINCT FROM (to_jsonb(OLD)-'approved_by'-'approved_at'))
 OR (OLD.approved_at IS NOT NULL) THEN RAISE EXCEPTION 'Closed shifts cannot be altered'; END IF;
 RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER commerce_closed_shift BEFORE UPDATE OR DELETE ON commerce_shifts
 FOR EACH ROW EXECUTE FUNCTION commerce_guard_closed_shift();
ALTER TABLE commerce_stock_movements ADD COLUMN financial_command_id UUID;
ALTER TABLE commerce_stock_movements ADD CONSTRAINT commerce_stock_financial_command_fk
 FOREIGN KEY(business_id,financial_command_id) REFERENCES commerce_financial_commands(business_id,id);
ALTER TABLE commerce_stock_movements DROP CONSTRAINT commerce_movement_type_check;
ALTER TABLE commerce_stock_movements DROP CONSTRAINT commerce_movement_quantity_check;
ALTER TABLE commerce_stock_movements ADD CONSTRAINT commerce_movement_type_check CHECK(movement_type IN
 ('OPENING_STOCK','TRANSFER_OUT','TRANSFER_IN','ADJUSTMENT','PURCHASE_RECEIPT','SALE','SALE_RETURN','SUPPLIER_RETURN'));
ALTER TABLE commerce_stock_movements ADD CONSTRAINT commerce_movement_quantity_check CHECK
 ((movement_type IN ('OPENING_STOCK','TRANSFER_IN','PURCHASE_RECEIPT','SALE_RETURN') AND quantity_delta>0)
 OR (movement_type IN ('TRANSFER_OUT','SALE','SUPPLIER_RETURN') AND quantity_delta<0) OR (movement_type='ADJUSTMENT' AND quantity_delta<>0));
