ALTER TABLE commerce_documents ADD COLUMN business_name VARCHAR(200);
ALTER TABLE commerce_documents ADD COLUMN branch_name VARCHAR(200);
ALTER TABLE commerce_documents ADD COLUMN location_name VARCHAR(200);
ALTER TABLE commerce_documents ADD COLUMN cashier_name VARCHAR(200);
ALTER TABLE commerce_document_lines ADD COLUMN category_name VARCHAR(160);
ALTER TABLE commerce_documents ADD COLUMN created_txid BIGINT NOT NULL DEFAULT txid_current();
CREATE OR REPLACE FUNCTION commerce_guard_document_append() RETURNS trigger AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM commerce_documents WHERE business_id=NEW.business_id AND id=NEW.document_id AND created_txid=txid_current()) THEN
  RAISE EXCEPTION 'Cannot append lines to a posted document';
 END IF;
 RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER commerce_document_no_late_lines BEFORE INSERT ON commerce_document_lines
 FOR EACH ROW EXECUTE FUNCTION commerce_guard_document_append();
CREATE INDEX commerce_document_branch_history ON commerce_documents(business_id,branch_id,created_at);
CREATE INDEX commerce_document_party_history ON commerce_documents(business_id,party_id,kind);
CREATE INDEX commerce_payment_document ON commerce_payments(business_id,document_id);
CREATE INDEX commerce_returns_line ON commerce_returns(business_id,line_id);
CREATE INDEX commerce_receiving_line ON commerce_receipt_lines(business_id,line_id);
