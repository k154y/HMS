ALTER TABLE order_items ADD COLUMN preparation_status VARCHAR(20) NOT NULL DEFAULT 'SENT'
CHECK(preparation_status IN ('SENT','PREPARING','READY'));
UPDATE order_items i SET preparation_status=CASE WHEN o.status IN ('READY','SERVED') THEN 'READY' WHEN o.status='PREPARING' THEN 'PREPARING' ELSE 'SENT' END FROM orders o WHERE o.id=i.order_id;
CREATE TRIGGER credit_ledger_immutable BEFORE UPDATE OR DELETE ON credit_ledger FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
