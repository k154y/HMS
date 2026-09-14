CREATE TABLE platform_administrators (
 user_id UUID PRIMARY KEY REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE platform_audit_events (
 id UUID PRIMARY KEY,
 actor_id UUID NOT NULL REFERENCES users(id),
 hotel_id UUID NOT NULL REFERENCES hotels(id),
 action VARCHAR(100) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TRIGGER platform_audit_immutable BEFORE UPDATE OR DELETE ON platform_audit_events
 FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
