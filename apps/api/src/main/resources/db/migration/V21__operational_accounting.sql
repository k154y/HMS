ALTER TABLE order_items ADD COLUMN consumed_quantity NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK(consumed_quantity>=0);
CREATE TABLE vendor_payments (
 id UUID PRIMARY KEY, hotel_id UUID NOT NULL, branch_id UUID NOT NULL,
 purchase_order_id UUID NOT NULL, amount NUMERIC(19,4) NOT NULL CHECK(amount>0),
 method VARCHAR(30) NOT NULL CHECK(method IN ('CASH','MOBILE_MONEY','CARD','BANK_TRANSFER')),
 request_id UUID NOT NULL, actor_id UUID NOT NULL REFERENCES users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(purchase_order_id,hotel_id,branch_id) REFERENCES purchase_orders(id,hotel_id,branch_id),
 UNIQUE(hotel_id,branch_id,request_id)
);
CREATE TRIGGER vendor_payments_immutable BEFORE UPDATE OR DELETE ON vendor_payments FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
CREATE TABLE payment_approvals (
 id UUID PRIMARY KEY, hotel_id UUID NOT NULL, branch_id UUID NOT NULL, folio_id UUID NOT NULL,
 request_id UUID NOT NULL, requested_by UUID NOT NULL REFERENCES users(id), approved_by UUID REFERENCES users(id),
 payload TEXT NOT NULL, status VARCHAR(20) NOT NULL CHECK(status IN ('PENDING','APPROVED','REJECTED')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(folio_id,hotel_id,branch_id) REFERENCES folios(id,hotel_id,branch_id), UNIQUE(hotel_id,branch_id,request_id)
);
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE r.system_defined AND r.code='WAITER'
AND p.code IN ('CUSTOMER_VIEW','CUSTOMER_MANAGE','FOLIO_VIEW','PRODUCT_VIEW','PAYMENT_RECORD')
ON CONFLICT DO NOTHING;
