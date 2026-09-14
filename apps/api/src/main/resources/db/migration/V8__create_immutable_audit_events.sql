CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    hotel_id UUID REFERENCES hotels(id),
    branch_id UUID,
    actor_user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    request_id VARCHAR(64),
    CONSTRAINT fk_audit_branch FOREIGN KEY (branch_id, hotel_id) REFERENCES branches(id, hotel_id),
    CONSTRAINT chk_audit_branch_scope CHECK (branch_id IS NULL OR hotel_id IS NOT NULL)
);
CREATE INDEX idx_audit_hotel_time ON audit_events(hotel_id, occurred_at DESC, id);
CREATE INDEX idx_audit_branch ON audit_events(branch_id);
CREATE INDEX idx_audit_actor ON audit_events(actor_user_id);

CREATE FUNCTION hms_reject_history_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Historical records cannot be changed or deleted' USING ERRCODE = '23514';
END;
$$;
CREATE TRIGGER audit_events_immutable BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
