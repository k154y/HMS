CREATE TABLE expenses (
 id UUID PRIMARY KEY,hotel_id UUID NOT NULL,branch_id UUID NOT NULL,
 expense_date DATE NOT NULL,category VARCHAR(100) NOT NULL,description VARCHAR(1000) NOT NULL,
 amount NUMERIC(19,4) NOT NULL CHECK(amount>0),method VARCHAR(30) NOT NULL CHECK(method IN ('CASH','MOBILE_MONEY','CARD','BANK_TRANSFER')),
 request_id UUID NOT NULL,actor_id UUID NOT NULL REFERENCES users(id),created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(branch_id,hotel_id) REFERENCES branches(id,hotel_id),UNIQUE(hotel_id,branch_id,request_id)
);
CREATE TRIGGER expenses_immutable BEFORE UPDATE OR DELETE ON expenses FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
