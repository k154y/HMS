ALTER TABLE payment_approvals ADD COLUMN decision_reason VARCHAR(1000);
ALTER TABLE payment_approvals ADD COLUMN decided_at TIMESTAMPTZ;
ALTER TABLE audit_events ADD COLUMN old_value TEXT;
ALTER TABLE audit_events ADD COLUMN new_value TEXT;
ALTER TABLE audit_events ADD COLUMN reason VARCHAR(1000);
ALTER TABLE audit_events ADD COLUMN approval_status VARCHAR(30);
ALTER TABLE audit_events ADD COLUMN device VARCHAR(500);
ALTER TABLE audit_events ADD COLUMN remote_address VARCHAR(100);
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('MANAGER','ACCOUNTANT','SUPERVISOR') AND p.code IN ('CASHIER_SHIFT_OPEN','CASHIER_SHIFT_CLOSE')
ON CONFLICT DO NOTHING;
