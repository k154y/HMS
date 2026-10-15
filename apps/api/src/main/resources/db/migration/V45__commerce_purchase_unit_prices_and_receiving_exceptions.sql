-- Standard buying prices may be registered per package without requiring users to divide by the base unit.
ALTER TABLE commerce_product_units ADD COLUMN purchase_package_price NUMERIC(19,4)
  CHECK (purchase_package_price >= 0);

-- Record delivery discrepancies as immutable supplier claims. Damaged/missing goods
-- do NOT increase saleable inventory or automatically settle supplier liabilities.
CREATE TABLE commerce_receiving_exceptions (
  id UUID PRIMARY KEY,
  business_id UUID NOT NULL,
  command_id UUID NOT NULL,
  line_id UUID NOT NULL,
  damaged_quantity NUMERIC(19,6) NOT NULL DEFAULT 0 CHECK(damaged_quantity >= 0),
  missing_quantity NUMERIC(19,6) NOT NULL DEFAULT 0 CHECK(missing_quantity >= 0),
  note VARCHAR(500) NOT NULL,
  resolution VARCHAR(20) NOT NULL CHECK(resolution IN ('PENDING','REPLACEMENT','SUPPLIER_CREDIT')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(command_id,line_id),
  FOREIGN KEY (business_id,command_id) REFERENCES commerce_financial_commands(business_id,id),
  FOREIGN KEY (business_id,line_id) REFERENCES commerce_document_lines(business_id,id)
);
CREATE TRIGGER commerce_receiving_exceptions_immutable BEFORE UPDATE OR DELETE ON commerce_receiving_exceptions
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
CREATE INDEX commerce_receiving_exception_line ON commerce_receiving_exceptions(business_id,line_id);

-- Preserve the supplier document date separately from the time KBP posted it.
ALTER TABLE commerce_documents ADD COLUMN supply_date DATE;

-- Product-specific discount, return and divisibility policies. Existing products retain
-- conservative cashier permissions; managers still have explicit audited overrides.
ALTER TABLE commerce_products
  ADD COLUMN discount_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN cashier_discount_limit NUMERIC(7,4) NOT NULL DEFAULT 0 CHECK(cashier_discount_limit BETWEEN 0 AND 100),
  ADD COLUMN return_window_days INTEGER NOT NULL DEFAULT 7 CHECK(return_window_days BETWEEN 0 AND 3650),
  ADD COLUMN fractional_units BOOLEAN NOT NULL DEFAULT TRUE;

-- Preserve the product return window on posted sale lines, even if catalog rules change.
ALTER TABLE commerce_document_lines ADD COLUMN return_window_days INTEGER NOT NULL DEFAULT 7
 CHECK (return_window_days BETWEEN 0 AND 3650);
