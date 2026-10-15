ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE kbp_provisioning_audit (
 id UUID PRIMARY KEY,
 actor_id UUID NOT NULL REFERENCES users(id),
 owner_id UUID NOT NULL REFERENCES users(id),
 product VARCHAR(16) NOT NULL CHECK(product IN ('HOTEL','COMMERCE')),
 organization_id UUID NOT NULL,
 action VARCHAR(60) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TRIGGER kbp_provisioning_audit_immutable BEFORE UPDATE OR DELETE ON kbp_provisioning_audit
 FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
